package com.restonic4.logistics.screens;

/**
 * Immutable theme record for {@link PanelScreen}.
 *
 * <p>Every visual constant lives here so that any screen can trivially adopt a
 * different look by passing a custom instance to the {@code PanelScreen}
 * constructor — without touching layout or rendering logic.
 *
 * <p>Use {@link #DEFAULT} for the standard dark-green debug aesthetic, or call
 * {@link Builder#from(PanelScreenTheme)} to create a variant that only changes
 * the fields you care about.
 *
 * <p>Colour format: {@code 0xAARRGGBB}.
 */
public final class PanelScreenTheme {

    // -------------------------------------------------------------------------
    // Singleton default — matches the original EnergyDebugScreen palette
    // -------------------------------------------------------------------------

    public static final PanelScreenTheme DEFAULT = new Builder().build();

    // -------------------------------------------------------------------------
    // Layout metrics
    // -------------------------------------------------------------------------

    /** Outer padding around and between panels, in pixels. */
    public final int pad;

    /** Height of a single text line, in pixels. */
    public final int lineHeight;

    /** Vertical gap inserted between logical sections inside a panel. */
    public final int sectionGap;

    /** Fixed width of every panel, in pixels. */
    public final int panelWidth;

    /**
     * X-offset at which the value column starts inside a two-column row,
     * measured from the content left edge (i.e. {@code panelX + pad}).
     */
    public final int col2X;

    // -------------------------------------------------------------------------
    // Colours
    // -------------------------------------------------------------------------

    /** Panel background fill. */
    public final int colBackground;

    /** Panel border colour (four edges). */
    public final int colBorder;

    /**
     * Accent colour used for the top stripe inside every panel and for
     * section-header underlines.
     */
    public final int colAccent;

    /** Generic label / secondary text. */
    public final int colLabel;

    /** Generic value text. */
    public final int colValue;

    /** Section header text. */
    public final int colHeader;

    /** Warning colour (e.g. "no data available"). */
    public final int colWarn;

    /** Positive / good state indicator. */
    public final int colGood;

    /** Negative / bad state indicator. */
    public final int colBad;

    /** Neutral informational colour. */
    public final int colNeutral;

    /** Divider lines inside panels. */
    public final int colDivider;

    /** Dismiss-hint text colour. */
    public final int colHint;

    // -------------------------------------------------------------------------
    // Progress-bar specific colours
    // -------------------------------------------------------------------------

    /** Progress-bar background track. */
    public final int colBarTrack;

    /**
     * Progress-bar fill when the value is above {@link #barHighThreshold}
     * (healthy / high).
     */
    public final int colBarHigh;

    /**
     * Progress-bar fill when the value is between {@link #barLowThreshold} and
     * {@link #barHighThreshold} (moderate / warning).
     */
    public final int colBarMid;

    /**
     * Progress-bar fill when the value is below {@link #barLowThreshold}
     * (critical / low).
     */
    public final int colBarLow;

    /** Fraction above which the bar is drawn in {@link #colBarHigh}. */
    public final float barHighThreshold;

    /** Fraction below which the bar is drawn in {@link #colBarLow}. */
    public final float barLowThreshold;

    // -------------------------------------------------------------------------
    // Constructor (private — use Builder)
    // -------------------------------------------------------------------------

    private PanelScreenTheme(Builder b) {
        this.pad               = b.pad;
        this.lineHeight        = b.lineHeight;
        this.sectionGap        = b.sectionGap;
        this.panelWidth        = b.panelWidth;
        this.col2X             = b.col2X;
        this.colBackground     = b.colBackground;
        this.colBorder         = b.colBorder;
        this.colAccent         = b.colAccent;
        this.colLabel          = b.colLabel;
        this.colValue          = b.colValue;
        this.colHeader         = b.colHeader;
        this.colWarn           = b.colWarn;
        this.colGood           = b.colGood;
        this.colBad            = b.colBad;
        this.colNeutral        = b.colNeutral;
        this.colDivider        = b.colDivider;
        this.colHint           = b.colHint;
        this.colBarTrack       = b.colBarTrack;
        this.colBarHigh        = b.colBarHigh;
        this.colBarMid         = b.colBarMid;
        this.colBarLow         = b.colBarLow;
        this.barHighThreshold  = b.barHighThreshold;
        this.barLowThreshold   = b.barLowThreshold;
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static final class Builder {

        // Layout defaults
        private int   pad              = 8;
        private int   lineHeight       = 10;
        private int   sectionGap       = 5;
        private int   panelWidth       = 260;
        private int   col2X            = 130;

        // Colour defaults — original dark-green energy palette
        private int   colBackground    = 0xD0101010;
        private int   colBorder        = 0xFF2A4A2A;
        private int   colAccent        = 0xFF44FF88;
        private int   colLabel         = 0xFF8FA88F;
        private int   colValue         = 0xFFE8FFE8;
        private int   colHeader        = 0xFFFFFFFF;
        private int   colWarn          = 0xFFFFAA33;
        private int   colGood          = 0xFF44FF88;
        private int   colBad           = 0xFFFF4444;
        private int   colNeutral       = 0xFF88CCFF;
        private int   colDivider       = 0xFF1A3A1A;
        private int   colHint          = 0xFF8FA88F;

        // Bar defaults
        private int   colBarTrack      = 0xFF1A2A1A;
        private int   colBarHigh       = 0xFF44CC66;
        private int   colBarMid        = 0xFFCCA030;
        private int   colBarLow        = 0xFFCC3333;
        private float barHighThreshold = 0.5f;
        private float barLowThreshold  = 0.2f;

        public Builder() {}

        /**
         * Seeds this builder from an existing theme so you only need to
         * override the fields that differ.
         *
         * <pre>{@code
         * PanelScreenTheme redTheme = PanelScreenTheme.Builder
         *         .from(PanelScreenTheme.DEFAULT)
         *         .colAccent(0xFFFF4444)
         *         .colBorder(0xFF4A2A2A)
         *         .build();
         * }</pre>
         */
        public static Builder from(PanelScreenTheme base) {
            Builder b = new Builder();
            b.pad              = base.pad;
            b.lineHeight       = base.lineHeight;
            b.sectionGap       = base.sectionGap;
            b.panelWidth       = base.panelWidth;
            b.col2X            = base.col2X;
            b.colBackground    = base.colBackground;
            b.colBorder        = base.colBorder;
            b.colAccent        = base.colAccent;
            b.colLabel         = base.colLabel;
            b.colValue         = base.colValue;
            b.colHeader        = base.colHeader;
            b.colWarn          = base.colWarn;
            b.colGood          = base.colGood;
            b.colBad           = base.colBad;
            b.colNeutral       = base.colNeutral;
            b.colDivider       = base.colDivider;
            b.colHint          = base.colHint;
            b.colBarTrack      = base.colBarTrack;
            b.colBarHigh       = base.colBarHigh;
            b.colBarMid        = base.colBarMid;
            b.colBarLow        = base.colBarLow;
            b.barHighThreshold = base.barHighThreshold;
            b.barLowThreshold  = base.barLowThreshold;
            return b;
        }

        // --- fluent setters ---------------------------------------------------

        public Builder pad(int v)               { this.pad              = v; return this; }
        public Builder lineHeight(int v)         { this.lineHeight       = v; return this; }
        public Builder sectionGap(int v)         { this.sectionGap       = v; return this; }
        public Builder panelWidth(int v)         { this.panelWidth       = v; return this; }
        public Builder col2X(int v)              { this.col2X            = v; return this; }
        public Builder colBackground(int v)      { this.colBackground    = v; return this; }
        public Builder colBorder(int v)          { this.colBorder        = v; return this; }
        public Builder colAccent(int v)          { this.colAccent        = v; return this; }
        public Builder colLabel(int v)           { this.colLabel         = v; return this; }
        public Builder colValue(int v)           { this.colValue         = v; return this; }
        public Builder colHeader(int v)          { this.colHeader        = v; return this; }
        public Builder colWarn(int v)            { this.colWarn          = v; return this; }
        public Builder colGood(int v)            { this.colGood          = v; return this; }
        public Builder colBad(int v)             { this.colBad           = v; return this; }
        public Builder colNeutral(int v)         { this.colNeutral       = v; return this; }
        public Builder colDivider(int v)         { this.colDivider       = v; return this; }
        public Builder colHint(int v)            { this.colHint          = v; return this; }
        public Builder colBarTrack(int v)        { this.colBarTrack      = v; return this; }
        public Builder colBarHigh(int v)         { this.colBarHigh       = v; return this; }
        public Builder colBarMid(int v)          { this.colBarMid        = v; return this; }
        public Builder colBarLow(int v)          { this.colBarLow        = v; return this; }
        public Builder barHighThreshold(float v) { this.barHighThreshold = v; return this; }
        public Builder barLowThreshold(float v)  { this.barLowThreshold  = v; return this; }

        public PanelScreenTheme build() {
            return new PanelScreenTheme(this);
        }
    }
}