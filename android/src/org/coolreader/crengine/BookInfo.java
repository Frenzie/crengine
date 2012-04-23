package org.coolreader.crengine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.util.Log;

public class BookInfo {
	private FileInfo fileInfo;
	private Bookmark lastPosition;
	private ArrayList<Bookmark> bookmarks = new ArrayList<Bookmark>();

	synchronized public void setShortcutBookmark(int shortcut, Bookmark bookmark)
	{
		bookmark.setShortcut(shortcut);
		for ( int i=0; i<bookmarks.size(); i++ ) {
			Bookmark bm = bookmarks.get(i);
			if ( bm.getType()==Bookmark.TYPE_POSITION && bm.getShortcut()==shortcut ) {
				bookmark.setId(bm.getId());
				bookmarks.set(i, bookmark);
				return;
			}
		}
		bookmarks.add(bookmark);
	}
	
	synchronized public Bookmark findShortcutBookmark( int shortcut )
	{
		for ( Bookmark bm : bookmarks )
			if ( bm.getType()==Bookmark.TYPE_POSITION && bm.getShortcut()==shortcut )
				return bm;
		return null;
	}
	
	public void updateAccess()
	{
		if (lastPosition != null) {
			lastPosition.setTimeStamp(System.currentTimeMillis());
		}
	}
	
	/**
	 * Deep copy.
	 * @param bookInfo is source object to copy from.
	 */
	public BookInfo(BookInfo bookInfo) {
		this.fileInfo = new FileInfo(bookInfo.fileInfo);
		if (bookInfo.lastPosition != null)
			this.lastPosition = new Bookmark(bookInfo.lastPosition);
		for (int i=0; i < bookInfo.getBookmarkCount(); i++) {
			this.addBookmark(new Bookmark(bookInfo.getBookmark(i)));
		}
	}
	
	public BookInfo(FileInfo fileInfo)
	{
		this.fileInfo = fileInfo; //new FileInfo(fileInfo);
	}
	
	public Bookmark getLastPosition()
	{
		return lastPosition;
	}
	
	public void setLastPosition( Bookmark position )
	{
		synchronized (this) {
			if ( lastPosition!=null ) {
				if (position.getStartPos()!=null && position.getStartPos().equals(lastPosition.getStartPos()))
					return; // not changed
				position.setId(lastPosition.getId());
			}
			lastPosition = position;
			fileInfo.lastAccessTime = lastPosition.getTimeStamp();
		}
	}
	
	public FileInfo getFileInfo()
	{
		return fileInfo;
	}
	
	synchronized public void addBookmark( Bookmark bm )
	{
		bookmarks.add(bm);
	}

	synchronized public int getBookmarkCount()
	{
		return bookmarks.size();
	}

	synchronized public Bookmark getBookmark( int index )
	{
		return bookmarks.get(index);
	}

	synchronized public ArrayList<Bookmark> getAllBookmarks()
	{
		ArrayList<Bookmark> list = new ArrayList<Bookmark>(bookmarks.size() + 1);
		list.addAll(bookmarks);
		if (lastPosition != null)
			list.add(lastPosition);
		return list;
	}

	synchronized public Bookmark findBookmark(Bookmark bm)
	{
		if ( bm==null )
			return null;
		int index = findBookmarkIndex(bm);
		if (index < 0)
			return null;
		return bookmarks.get(index);
	}

	private int findBookmarkIndex(Bookmark bm)
	{
		if ( bm==null )
			return -1;
		int index = -1;
		for ( int i=0; i<bookmarks.size(); i++ ) {
			Bookmark item = bookmarks.get(i);
			if (bm.getType() == Bookmark.TYPE_LAST_POSITION && item.getType() == Bookmark.TYPE_LAST_POSITION) {
				index = i;
				break;
			}
			if ( bm.getShortcut()>0 && item.getShortcut()==bm.getShortcut() ) {
				index = i;
				break;
			}
			if ( bm.getStartPos()!=null && bm.getStartPos().equals(item.getStartPos())) {
				if (bm.getType() == Bookmark.TYPE_POSITION) {
					index = i;
					break;
				}
				if (bm.getEndPos()!=null && bm.getEndPos().equals(item.getEndPos())) {
					if (item.getId() != null && bm.getId() != null && !bm.getId().equals(item.getId()))
						continue; // another bookmark with same pos
					index = i;
					break;
				}
			}
		}
		return index;
	}

	synchronized public Bookmark syncBookmark(Bookmark bm)
	{
		if ( bm==null )
			return null;
		int index = findBookmarkIndex(bm);
		if ( index<0 ) {
			addBookmark(bm);
			return bm;
		}
		Bookmark item = bookmarks.get(index);
		if (item.getTimeStamp() >= bm.getTimeStamp())
			return null;
		item.setType(bm.getType());
		item.setTimeStamp(bm.getTimeStamp());
		item.setPosText(bm.getPosText());
		item.setCommentText(bm.getCommentText());
		return item;
	}

	synchronized public Bookmark updateBookmark(Bookmark bm)
	{
		if ( bm==null )
			return null;
		int index = findBookmarkIndex(bm);
		if ( index<0 ) {
			Log.e("cr3", "cannot find bookmark " + bm);
			return null;
		}
		Bookmark item = bookmarks.get(index);
		item.setTimeStamp(bm.getTimeStamp());
		item.setPosText(bm.getPosText());
		item.setCommentText(bm.getCommentText());
		return item;
	}
	synchronized public Bookmark removeBookmark(Bookmark bm)
	{
		if ( bm==null )
			return null;
		int index = findBookmarkIndex(bm);
		if ( index<0 ) {
			Log.e("cr3", "cannot find bookmark " + bm);
			return null;
		}
		return bookmarks.remove(index);
	}

	synchronized public void sortBookmarks() {
		Collections.sort(bookmarks, new Comparator<Bookmark>() {
			@Override
			public int compare(Bookmark bm1, Bookmark bm2) {
				if ( bm1.getPercent() < bm2.getPercent() )
					return -1;
				if ( bm1.getPercent() > bm2.getPercent() )
					return 1;
				return 0;
			}
		});
	}
	
	synchronized public String getBookmarksExportText() {
		StringBuilder buf = new StringBuilder();
		File pathname = new File(fileInfo.getPathName());
		buf.append("# file name: " + pathname.getName() + "\n");
		buf.append("# file path: " + pathname.getParent() + "\n");
		buf.append("# book title: " + fileInfo.title + "\n");
		buf.append("# author: " + fileInfo.authors + "\n");
		buf.append("\n");
		for ( Bookmark bm : bookmarks ) {
			if ( bm.getType()!=Bookmark.TYPE_COMMENT && bm.getType()!=Bookmark.TYPE_CORRECTION )
				continue;
			int percent = bm.getPercent();
			String ps = String.valueOf(percent%100);
			if ( ps.length()<2 )
				ps = "0" + ps;
			ps = String.valueOf(percent/100) + "." + ps  + "%";
			buf.append("## " + ps + " - " + (bm.getType()!=Bookmark.TYPE_COMMENT ? "comment" : "correction")  + "\n");
			if ( bm.getTitleText()!=null )
				buf.append("## " + bm.getTitleText() + "\n");
			if ( bm.getPosText()!=null )
				buf.append("<< " + bm.getPosText() + "\n");
			if ( bm.getCommentText()!=null )
				buf.append(">> " + bm.getCommentText() + "\n");
			buf.append("\n");
		}
		return buf.toString();
	}

	synchronized public boolean exportBookmarks( String fileName ) {
		Log.i("cr3", "Exporting bookmarks to file " + fileName);
		try { 
			FileOutputStream stream = new FileOutputStream(new File(fileName));
			OutputStreamWriter writer = new OutputStreamWriter(stream, "UTF-8"); 
			writer.write(0xfeff);
			writer.write("# Cool Reader 3 - exported bookmarks\r\n");
			File pathname = new File(fileInfo.getPathName());
			writer.write("# file name: " + pathname.getName() + "\r\n");
			writer.write("# file path: " + pathname.getParent() + "\r\n");
			writer.write("# book title: " + fileInfo.title + "\r\n");
			writer.write("# author: " + fileInfo.authors + "\r\n");
			writer.write("# series: " + fileInfo.series + "\r\n");
			writer.write("\r\n");
			for ( Bookmark bm : bookmarks ) {
				if ( bm.getType()!=Bookmark.TYPE_COMMENT && bm.getType()!=Bookmark.TYPE_CORRECTION )
					continue;
				int percent = bm.getPercent();
				String ps = String.valueOf(percent%100);
				if ( ps.length()<2 )
					ps = "0" + ps;
				ps = String.valueOf(percent/100) + "." + ps  + "%";
				writer.write("## " + ps + " - " + (bm.getType()==Bookmark.TYPE_COMMENT ? "comment" : "correction")  + "\r\n");
				if ( bm.getTitleText()!=null )
					writer.write("## " + bm.getTitleText() + "\r\n");
				if ( bm.getPosText()!=null )
					writer.write("<< " + bm.getPosText() + "\r\n");
				if ( bm.getCommentText()!=null )
					writer.write(">> " + bm.getCommentText() + "\r\n");
				writer.write("\r\n");
			}
			writer.close();
			return true;
		} catch ( IOException e ) {
			Log.e("cr3", "Cannot write bookmark file " + fileName);
			return false;
		}
	}
	
	
	synchronized public Bookmark removeBookmark( int index )
	{
		return bookmarks.remove(index);
	}
	
	synchronized public void setBookmarks(ArrayList<Bookmark> list)
	{
		if ( list.size()>0 ) {
			if ( list.get(0).getType()==0 ) {
				lastPosition = list.remove(0); 
			}
		}
		if ( list.size()>0 ) {
			bookmarks = list;
		}
	}

	@Override
	public String toString() {
		return "BookInfo [fileInfo=" + fileInfo + ", lastPosition="
				+ lastPosition + "]";
	}

	
	
}
