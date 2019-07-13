/** \file lvrend.h
    \brief DOM document rendering (formatting) functions

    CoolReader Engine

    (c) Vadim Lopatin, 2000-2006
    This source code is distributed under the terms of
    GNU General Public License.

    See LICENSE file for details.

*/

#ifndef __LV_REND_H_INCLUDED__
#define __LV_REND_H_INCLUDED__

#include "lvtinydom.h"

/// returns true if styles are identical
bool isSameFontStyle( css_style_rec_t * style1, css_style_rec_t * style2 );
/// removes format data from node
void freeFormatData( ldomNode * node );
/// returns best suitable font for style
LVFontRef getFont(css_style_rec_t * style, int documentId);
/// initializes format data for node
void initFormatData( ldomNode * node );
/// initializes rendering method for node
int initRendMethod( ldomNode * node, bool recurseChildren, bool allowAutoboxing );
/// converts style to text formatting API flags
int styleToTextFmtFlags( const css_style_ref_t & style, int oldflags );
/// renders block as single text formatter object
void renderFinalBlock( ldomNode * node, LFormattedText * txform, RenderRectAccessor * fmt, int & flags, int ident, int line_h, int valign_dy=0, bool * is_link_start=NULL );
/// renders block which contains subblocks
int renderBlockElement( LVRendPageContext & context, ldomNode * node, int x, int y, int width );
/// renders table element
int renderTable( LVRendPageContext & context, ldomNode * element, int x, int y, int width, bool shrink_to_fit, int & fitted_width );
/// sets node style
void setNodeStyle( ldomNode * node, css_style_ref_t parent_style, LVFontRef parent_font );
/// copy style
void copystyle( css_style_ref_t sourcestyle, css_style_ref_t deststyle );

/// draws formatted document to drawing buffer
void DrawDocument( LVDrawBuf & drawbuf, ldomNode * node, int x0, int y0, int dx, int dy, int doc_x, int doc_y, int page_height, ldomMarkedRangeList * marks,
                   ldomMarkedRangeList * bookmarks = NULL);

// Estimate width of node when rendered:
//   maxWidth: width if it would be rendered on an infinite width area
//   minWidth: width with a wrap on all spaces (no hyphenation), so width taken by the longest word
// full function for recursive use:
void getRenderedWidths(ldomNode * node, int &maxWidth, int &minWidth, bool ignorePadding,
    int &curMaxWidth, int &curWordWidth, bool &collapseNextSpace, int &lastSpaceWidth, int indent);
// simpler function for first call:
void getRenderedWidths(ldomNode * node, int &maxWidth, int &minWidth, bool ignorePadding=false);

#define STYLE_FONT_EMBOLD_MODE_NORMAL 0
#define STYLE_FONT_EMBOLD_MODE_EMBOLD 300

/// set global document font style embolden mode (0=off, 300=on)
void LVRendSetFontEmbolden( int addWidth=STYLE_FONT_EMBOLD_MODE_EMBOLD );
/// get global document font style embolden mode
int LVRendGetFontEmbolden();

int measureBorder(ldomNode *enode,int border);
int lengthToPx( css_length_t val, int base_px, int base_em, bool unspecified_as_em=false );
int scaleForRenderDPI( int value );

#define BASE_CSS_DPI 96 // at 96 dpi, 1 css px = 1 screen px
#define DEF_RENDER_DPI 96
#define DEF_RENDER_SCALE_FONT_WITH_DPI 0
extern int gRenderDPI;
extern bool gRenderScaleFontWithDPI;
extern int gRootFontSize;

#define INTERLINE_SCALE_FACTOR_NO_SCALE 1024
#define INTERLINE_SCALE_FACTOR_SHIFT 10
extern int gInterlineScaleFactor;

extern int gRenderBlockRenderingFlags;

// Enhanced rendering flags
#define BLOCK_RENDERING_ENHANCED                           0x00000001
#define BLOCK_RENDERING_ALLOW_PAGE_BREAK_WHEN_NO_CONTENT   0x00000002 // Allow consecutive page breaks when only separated
                                                                      // by margin/padding/border.
// Vertical margins
#define BLOCK_RENDERING_COLLAPSE_VERTICAL_MARGINS          0x00000010 // Collapse vertical margins
#define BLOCK_RENDERING_ALLOW_VERTICAL_NEGATIVE_MARGINS    0x00000020 // Allow individual negative margins in the calculation, the
                                                                      // final collapsed margin is ensure to be zero or positive.
#define BLOCK_RENDERING_ALLOW_NEGATIVE_COLLAPSED_MARGINS   0x00000040 // Allow the final vertical collapsed margin to be negative
                                                                      // (may mess with page splitting and text selection).
// Horizontal margins
#define BLOCK_RENDERING_ENSURE_MARGIN_AUTO_ALIGNMENT       0x00000100 // Ensure CSS "margin: auto", for aligning blocks.
#define BLOCK_RENDERING_ALLOW_HORIZONTAL_NEGATIVE_MARGINS  0x00000200 // Allow negative margins (otherwise, they are set to 0)
#define BLOCK_RENDERING_ALLOW_HORIZONTAL_BLOCK_OVERFLOW    0x00000400 // Allow block content to overflow its block container.
#define BLOCK_RENDERING_ALLOW_HORIZONTAL_PAGE_OVERFLOW     0x00000800 // Allow block content to overflow the page rect, showing
                                                                      // in the margin, and possibly clipped out.
// Widths and heights
#define BLOCK_RENDERING_USE_W3C_BOX_MODEL                  0x00001000 // Use W3C box model (CSS width and height do not include
                                                                      // paddings and borders)
#define BLOCK_RENDERING_ALLOW_STYLE_W_H_ABSOLUTE_UNITS     0x00002000 // Allow widths and heights in absolute units (when ensured)
#define BLOCK_RENDERING_ENSURE_STYLE_WIDTH                 0x00004000 // Ensure CSS widths and heights on all elements (otherwise
#define BLOCK_RENDERING_ENSURE_STYLE_HEIGHT                0x00008000 // only on <HR> and images, and when sizing floats).
// Floats
#define BLOCK_RENDERING_WRAP_FLOATS                        0x00010000 // Wrap floats in an internal floatBox element.
#define BLOCK_RENDERING_PREPARE_FLOATBOXES                 0x00020000 // Avoid style hash mismatch when toggling FLOAT_FLOATBOXES,
                                                                      // but make embedded floats inline when no more floating.
#define BLOCK_RENDERING_FLOAT_FLOATBOXES                   0x00040000 // Actually render floatBoxes floating.
// These 2, although allowing a more correct rendering of floats, can impact drawing performances and text/links selection:
#define BLOCK_RENDERING_DO_NOT_CLEAR_OWN_FLOATS            0x00100000 // Prevent blocks from clearing their own floats.
#define BLOCK_RENDERING_ALLOW_EXACT_FLOATS_FOOTPRINTS      0x00200000 // When 5 or less outer floats have impact on a final
                                                                      // block, store their ids instead of the 2 top left/right
                                                                      // rectangle, allowing text layout staircase-like.
// Enable everything
#define BLOCK_RENDERING_FULL_FEATURED                      0x7FFFFFFF

#define BLOCK_RENDERING_G(f) ( gRenderBlockRenderingFlags & BLOCK_RENDERING_##f )
#define BLOCK_RENDERING(v, f) ( v & BLOCK_RENDERING_##f )

#define DEF_RENDER_BLOCK_RENDERING_FLAGS BLOCK_RENDERING_FULL_FEATURED

int validateBlockRenderingFlags( int f );

#endif
