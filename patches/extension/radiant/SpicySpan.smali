.class public final Lradiant/SpicySpan;
.super Ljava/lang/Object;

# Spicy Lyrics span: colour, size, baseline lift, glow (shadow) and letter spacing in one SpanStyle
# make(colour, em size, lift as a fraction of the font size, glow ARGB, glow blur px, letter spacing em)
# Same constructor call as WordLyrics.makeSpan / emSpan, only the mask differs:
# bits 0,1,7,8,13 clear = colour, fontSize, letterSpacing, baselineShift, shadow provided -> 0xDE7C


.method private constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static make(JFFIFF)Landroidx/compose/ui/text/SpanStyle;
    .locals 23

    # p0,p1 = colour | p2 = em | p3 = lift | p4 = glow argb | p5 = blur | p6 = letter spacing
    # Shadow(color, offset = 0, blur) through the defaults constructor, mask 0 = all given
    new-instance v1, Landroidx/compose/ui/graphics/Shadow;

    move/from16 v10, p4

    invoke-static {v10}, Landroidx/compose/ui/graphics/ColorKt;->Color(I)J

    move-result-wide v2

    const-wide/16 v4, 0x0

    move/from16 v6, p5

    const/4 v7, 0x0

    const/4 v8, 0x0

    invoke-direct/range {v1 .. v8}, Landroidx/compose/ui/graphics/Shadow;-><init>(JJFILkotlin/jvm/internal/DefaultConstructorMarker;)V

    move-object v18, v1

    # letterSpacing (em)
    move/from16 v12, p6

    invoke-static {v12}, Landroidx/compose/ui/unit/TextUnitKt;->getEm(F)J

    move-result-wide v10

    # baselineShift (boxed)
    move/from16 v12, p3

    invoke-static {v12}, Landroidx/compose/ui/text/style/BaselineShift;->box-impl(F)Landroidx/compose/ui/text/style/BaselineShift;

    move-result-object v12

    # fontSize (em)
    move/from16 v13, p2

    invoke-static {v13}, Landroidx/compose/ui/unit/TextUnitKt;->getEm(F)J

    move-result-wide v3

    # colour
    move-wide/from16 v1, p0

    const/4 v5, 0x0

    const/4 v6, 0x0

    const/4 v7, 0x0

    const/4 v8, 0x0

    const/4 v9, 0x0

    const/4 v13, 0x0

    const/4 v14, 0x0

    const-wide/16 v15, 0x0

    const/16 v17, 0x0

    const/16 v19, 0x0

    const/16 v20, 0x0

    const v21, 0xde7c

    const/16 v22, 0x0

    new-instance v0, Landroidx/compose/ui/text/SpanStyle;

    invoke-direct/range {v0 .. v22}, Landroidx/compose/ui/text/SpanStyle;-><init>(JJLandroidx/compose/ui/text/font/FontWeight;Landroidx/compose/ui/text/font/FontStyle;Landroidx/compose/ui/text/font/FontSynthesis;Landroidx/compose/ui/text/font/FontFamily;Ljava/lang/String;JLandroidx/compose/ui/text/style/BaselineShift;Landroidx/compose/ui/text/style/TextGeometricTransform;Landroidx/compose/ui/text/intl/LocaleList;JLandroidx/compose/ui/text/style/TextDecoration;Landroidx/compose/ui/graphics/Shadow;Landroidx/compose/ui/text/PlatformSpanStyle;Landroidx/compose/ui/graphics/drawscope/DrawStyle;ILkotlin/jvm/internal/DefaultConstructorMarker;)V

    return-object v0
.end method
