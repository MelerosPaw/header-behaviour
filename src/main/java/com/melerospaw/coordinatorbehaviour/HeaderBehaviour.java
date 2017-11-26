package com.melerospaw.coordinatorbehaviour;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

public class HeaderBehaviour extends CoordinatorLayout.Behavior<LinearLayout> {

    private static final float EXPANDED_BOTTOM_MARGIN = 32f;
    private static final float EXPANDED_LEFT_MARGIN = 32f;

    private CoordinatorLayout parent;
    private AppBarLayout dependency;
    private Toolbar toolbar;
    private LinearLayout toolbarTitleContainer;
    private LinearLayout child;
    private TextView title;
    private TextView subtitle;

    private float expandedStartMargin;
    private float expandedBottomMargin;
    private float expandedTitleTextSize;
    private float expandedSubtitleTextSize;
    private float collapsedTitleTextSize;
    private float collapsedSubtitleTextSize;
    private float expandedSpaceBetweenLines;
    private boolean isSpaceBetweenLinesEnabled;
    private boolean interpolateTextColor;
    @ColorInt private int collapsedTitleTextColor;
    @ColorInt private int expandedTitleTextColor;
    @ColorInt private int collapsedSubtitleTextColor;
    @ColorInt private int expandedSubtitleTextColor;

    public HeaderBehaviour() {
        super();
    }

    public HeaderBehaviour(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.HeaderBehaviour);
        expandedTitleTextSize = attributes.getDimension(R.styleable.HeaderBehaviour_behaviour_expandedTitleTextSize, -1);
        expandedSubtitleTextSize = attributes.getDimension(R.styleable.HeaderBehaviour_behaviour_expandedSubtitleTextSize, -1);
        collapsedTitleTextSize = attributes.getDimension(R.styleable.HeaderBehaviour_behaviour_collapsedTitleTextSize, -1);
        collapsedSubtitleTextSize = attributes.getDimension(R.styleable.HeaderBehaviour_behaviour_collapsedSubtitleTextSize, -1);
        expandedStartMargin = attributes.getDimension(R.styleable.HeaderBehaviour_behaviour_expandedStartMargin, -1);
        expandedBottomMargin = attributes.getDimension(R.styleable.HeaderBehaviour_behaviour_expandedBottomMargin, -1);
        expandedSpaceBetweenLines = attributes.getDimension(R.styleable.HeaderBehaviour_behaviour_expandedSpaceBetweenLines, -1);
        @ColorInt int collapsedTextColor = attributes.getColor(R.styleable.HeaderBehaviour_behaviour_collapsedTextColor,
                ContextCompat.getColor(context, android.R.color.white));
        @ColorInt int expandedTextColor = attributes.getColor(R.styleable.HeaderBehaviour_behaviour_expandedTextColor,
                ContextCompat.getColor(context, android.R.color.black));
        collapsedTitleTextColor = attributes.getColor(R.styleable.HeaderBehaviour_behaviour_collapsedTitleColor, collapsedTextColor);
        expandedTitleTextColor = attributes.getColor(R.styleable.HeaderBehaviour_behaviour_expandedTitleColor, expandedTextColor);
        collapsedSubtitleTextColor = attributes.getColor(R.styleable.HeaderBehaviour_behaviour_collapsedSubtitleColor, collapsedTextColor);
        expandedSubtitleTextColor = attributes.getColor(R.styleable.HeaderBehaviour_behaviour_expandedSubtitleColor, expandedTextColor);
        interpolateTextColor = attributes.getBoolean(R.styleable.HeaderBehaviour_behaviour_interpolateTextColor, true);

        expandedTitleTextSize = expandedTitleTextSize != -1 ?
                pxToDp(expandedTitleTextSize, context) : -1;
        collapsedTitleTextSize = collapsedTitleTextSize != -1 ?
                pxToDp(collapsedTitleTextSize, context) : -1;
        expandedSubtitleTextSize = expandedSubtitleTextSize != -1 ?
                pxToDp(expandedSubtitleTextSize, context) : -1;
        collapsedSubtitleTextSize = collapsedSubtitleTextSize != -1 ?
                pxToDp(collapsedSubtitleTextSize, context) : -1;
        expandedStartMargin = expandedStartMargin == -1 ?
                EXPANDED_LEFT_MARGIN : pxToDp(expandedStartMargin, context);
        expandedBottomMargin = expandedBottomMargin == -1 ?
                EXPANDED_BOTTOM_MARGIN : pxToDp(expandedBottomMargin, context);
        isSpaceBetweenLinesEnabled = expandedSpaceBetweenLines != -1;

        attributes.recycle();
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, LinearLayout child, View dependency) {
        // En qué vista se tiene que fijar. Solo vale indicar vistas hijas directas del CoordinatorLayout
        return dependency instanceof AppBarLayout;
    }


    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, LinearLayout child, View dependency) {
        // Qué tienes que hacer cuando la vista cambie
        // Un behaviour solo se puede asignar a hijos directos del CoordinatorLayout.
        // El texto del título de la AppBar no cambia aunque se mueva con el CollapsingToolbarLayout
        // El CollapsingToolbarLayout tampoco se mueve nunca. Su tamaño siempre es extendido.
        this.parent = parent;
        this.child = child;
        this.dependency = (AppBarLayout) dependency;

        obtainViews();
        changeTitleTextSize();
        changeSubtitleTextSize();
        changeSubtitleMarginTop();
        changeTextColor();
        changePosition();
        toggleToolbarTitleVisibility();

        return true;
    }

    private void obtainViews() {
        CollapsingToolbarLayout collapsingToolbarLayout = findViewByClass(this.dependency, CollapsingToolbarLayout.class);
        if (collapsingToolbarLayout == null) {
            illegalStateException();
        } else {
            collapsingToolbarLayout.setTitle(" ");
            toolbar = findViewByClass(collapsingToolbarLayout, Toolbar.class);
            if (toolbar == null) {
                illegalStateException();
            } else {
                List<LinearLayout> linearLayouts = findViewsByClass(toolbar, LinearLayout.class);
                for (LinearLayout linearLayout : linearLayouts) {
                    if (linearLayout.getChildAt(0) == null || !(linearLayout.getChildAt(0) instanceof TextView)
                            || linearLayout.getChildAt(1) == null || !(linearLayout.getChildAt(1) instanceof TextView)) {
                        illegalStateException();
                    } else {
                        toolbarTitleContainer = linearLayout;
                    }
                }

                if (toolbarTitleContainer == null) {
                    illegalStateException();
                } else {
                    if (collapsedTitleTextSize == -1) {
                        collapsedTitleTextSize = pxToDp(((TextView) toolbarTitleContainer.getChildAt(0)).getTextSize(),
                                toolbarTitleContainer.getContext());
                    } else {
                        ((TextView) toolbarTitleContainer.getChildAt(0)).setTextSize(collapsedTitleTextSize);
                    }

                    if (collapsedSubtitleTextSize == -1) {
                        collapsedSubtitleTextSize = pxToDp(((TextView) toolbarTitleContainer.getChildAt(1)).getTextSize(),
                                toolbarTitleContainer.getContext());
                    } else {
                        ((TextView) toolbarTitleContainer.getChildAt(1)).setTextSize(collapsedSubtitleTextSize);
                    }
                }
            }
        }

        if (child.getChildAt(0) == null || !(child.getChildAt(0) instanceof TextView)
                || child.getChildAt(1) == null || !(child.getChildAt(1) instanceof TextView)) {
            illegalStateException();
        } else {
            title = (TextView) child.getChildAt(0);
            subtitle = (TextView) child.getChildAt(1);
            if (expandedTitleTextSize == -1) {
                expandedTitleTextSize = pxToDp(title.getTextSize(), title.getContext());
            } else {
                title.setTextSize(expandedTitleTextSize);
            }
            if (expandedSubtitleTextSize == -1) {
                expandedSubtitleTextSize = pxToDp(subtitle.getTextSize(), subtitle.getContext());
            } else {
                subtitle.setTextSize(expandedSubtitleTextSize);
            }
        }
    }

    private void changeTitleTextSize() {
        // Averigua el rango de tamaño de letra entre el tamaño mayor y el menor
        float textSizeRange = expandedTitleTextSize - collapsedTitleTextSize;
        float textSizeDifference = textSizeRange * getYScrolledPercentage() / 100;
        float proportionedTextSize = collapsedTitleTextSize + textSizeDifference;
        title.setTextSize(proportionedTextSize);
    }

    private void changeSubtitleTextSize() {
        // Averigua el rango de tamaño de letra entre el tamaño mayor y el menor
        float textSizeRange = expandedSubtitleTextSize - collapsedSubtitleTextSize;
        float textSizeDifference = textSizeRange * getYScrolledPercentage() / 100;
        float proportionedTextSize = collapsedSubtitleTextSize + textSizeDifference;
        subtitle.setTextSize(proportionedTextSize);
    }

    private void changeSubtitleMarginTop() {
        if (isSpaceBetweenLinesEnabled) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) subtitle.getLayoutParams();
            params.topMargin = (int) (getYScrolledPercentage() * expandedSpaceBetweenLines / 100);
        }
    }

    private void changeTextColor() {
        if (interpolateTextColor) {
            @ColorInt int titleColor = (Integer) new ArgbEvaluator().evaluate(
                    getYScrolledPercentage() / 100, collapsedTitleTextColor, expandedTitleTextColor);
            @ColorInt int subtitleColor = (Integer) new ArgbEvaluator().evaluate(
                    getYScrolledPercentage() / 100, collapsedSubtitleTextColor, expandedSubtitleTextColor);
            title.setTextColor(titleColor);
            subtitle.setTextColor(subtitleColor);
        }
    }

    private void changePosition() {
        child.setY(getYPosition());
        child.setX(getXPosition());
    }

    private float getYPosition() {
        child.measure(View.MeasureSpec.UNSPECIFIED, child.getHeight());
        return dependency.getBottom()
                - child.getMeasuredHeight()
                - getToolbarTitleBottomMargin()
                - getBottomDistancePercentage();
    }

    private int getToolbarTitleBottomMargin() {
        // Obtiene el margen inferior que hay desde el título de la toolbar hasta el borde.
        // Lo más lógico sería mirar cuánto hay desde el lado inferior del título hasta el lado
        // inferior de la toolbar y eso sería el margen. Pero resulta que durante la animación,
        // la toolbar cambia de posición pero el título no, entonces no podemos calcularlo así.
        // Además, la posición de la Toolbar cambia como si se estuviera moviendo hacia arriba en
        // lugar de hacia abajo. No lo entiendo. En fin, como la altura de la Toolbar no cambia y
        // la posición del título tampoco y están en la parte de arriba de la pantalla, podemos
        // averiguar la distancia entre el título y el lado inferior de la Toolbar usando la
        // altura. getBottom() nos devolverá la posición del lado inferior del título, al restársela
        // a la altura de la toolbar, sabremos cuánto hay.
        return toolbar.getHeight() - toolbarTitleContainer.getBottom();
    }

    private float getBottomDistancePercentage() {
        // Añade un extra de margen inferior para separar la vista del borde inferior.
        return expandedBottomMargin * getYScrolledPercentage() / 100;
    }

    // La vista se desplaza de arriba a abajo una distancia equivalente a appbar.getTotalScrollRange().
    // En cada moment del scroll hay que averiguar el porcentaje de scroll y aplicárselo a la
    // distancia total que se puede desplazar el texto de izquierda a derecha.
    private float getXPosition() {

        // Averigua la distancia total de scroll horizontal
        float collapsedXPosition = toolbarTitleContainer.getX();
        float expandedXPosition = dpToPixels(expandedStartMargin, dependency.getContext());
        float totalXScrollRange = collapsedXPosition - expandedXPosition;

        // Averigua el porcentaje de distancia vertical recorrido
        float yScrolledPercentage = getYScrolledPercentage();

        // Averigua y aplica la distancia horizontal que tiene que recorrer
        float xPorportionedRangeToBeScrolled = totalXScrollRange * yScrolledPercentage / 100;
        return collapsedXPosition - xPorportionedRangeToBeScrolled;
    }

    private float getYScrolledPercentage() {
        int yScrolledPixels = dependency.getTotalScrollRange() + dependency.getTop();
        return ((yScrolledPixels * 100) / (float) dependency.getTotalScrollRange());
    }

    private void toggleToolbarTitleVisibility() {
        if (isToolbarCollapsed(dependency)) {
            toolbarTitleContainer.setVisibility(View.VISIBLE);
        } else {
            toolbarTitleContainer.setVisibility(View.GONE);
        }
    }

    private boolean isToolbarCollapsed(AppBarLayout appBarLayout) {
        // Cuando la toolbar está contraída por completo, la parte superior de la AppBar está a
        // -rangoTotalScroll. Es decir, que si la AppBar tiene una distancia de scroll de 200,
        // cuando la Toolbar está contraída, el borde superior está a -200.
        return -appBarLayout.getTop() == appBarLayout.getTotalScrollRange();
    }

    private static float dpToPixels(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    private static float pxToDp(float px, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return px / ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    @Nullable
    private <T extends View> T findViewByClass(ViewGroup viewGroup, Class clase){
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            if (clase.isInstance(viewGroup.getChildAt(i))) {
                return (T) viewGroup.getChildAt(i);
            }
        }
        return null;
    }

    private <T extends View> List<T> findViewsByClass(ViewGroup viewGroup, Class clase){

        List<T> views = new LinkedList<>();
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            if (clase.isInstance(viewGroup.getChildAt(i))) {
                views.add((T) viewGroup.getChildAt(i));
            }
        }
        return views;
    }

    private void illegalStateException() {
        throw new IllegalStateException(
                "Using HeaderBehaviour requires the following view structure xml:\n" +
                "  CoordinatorLayout\n"+
                "    +--- AppBarLayout\n" +
                "    |    +--- Toolbar\n" +
                "    |    \\--- LinearLayout\n" +
                "    |         +--- TextView (title)\n" +
                "    |         \\--- TextView (subtitle)\n" +
                "    \\--- LinearLayout\n" +
                "         +--- TextView (the same title)\n" +
                "         \\--- TextView (the same subitle)");
    }

    public void setExpandedStartMargin(float expandedStartMargin) {
        this.expandedStartMargin = expandedStartMargin;
        onDependentViewChanged(parent, child, dependency);
    }

    public void setExpandedBottomMargin(float expandedBottomMargin) {
        this.expandedBottomMargin = expandedBottomMargin;
        onDependentViewChanged(parent, child, dependency);
    }

    public void setExpandedTitleTextSize(float expandedTitleTextSize) {
        this.expandedTitleTextSize = expandedTitleTextSize;
        onDependentViewChanged(parent, child, dependency);
    }

    public void setExpandedSubtitleTextSize(float expandedSubtitleTextSize) {
        this.expandedSubtitleTextSize = expandedSubtitleTextSize;
        onDependentViewChanged(parent, child, dependency);
    }

    public void setCollapsedTitleTextSize(float collapsedTitleTextSize) {
        this.collapsedTitleTextSize = collapsedTitleTextSize;
        onDependentViewChanged(parent, child, dependency);
    }

    public void setCollapsedSubtitleTextSize(float collapsedSubtitleTextSize) {
        this.collapsedSubtitleTextSize = collapsedSubtitleTextSize;
        onDependentViewChanged(parent, child, dependency);
    }

    public void setExpandedSpaceBetweenLines(float expandedSpaceBetweenLines) {
        this.expandedSpaceBetweenLines = expandedSpaceBetweenLines;
        onDependentViewChanged(parent, child, dependency);
    }

    public void enableSpaceBetweenLines(boolean enableSpaceBetweenLines) {
        this.isSpaceBetweenLinesEnabled = enableSpaceBetweenLines;
        onDependentViewChanged(parent, child, dependency);
    }

    public void setInterpolateTextColor(boolean interpolateTextColor) {
        this.interpolateTextColor = interpolateTextColor;
        onDependentViewChanged(parent, child, dependency);
    }

    public void setCollapsedTitleTextColor(int collapsedTitleTextColor) {
        this.collapsedTitleTextColor = collapsedTitleTextColor;
        onDependentViewChanged(parent, child, dependency);
    }

    public void setExpandedTitleTextColor(int expandedTitleTextColor) {
        this.expandedTitleTextColor = expandedTitleTextColor;
        onDependentViewChanged(parent, child, dependency);
    }

    public void setCollapsedSubtitleTextColor(int collapsedSubtitleTextColor) {
        this.collapsedSubtitleTextColor = collapsedSubtitleTextColor;
        onDependentViewChanged(parent, child, dependency);
    }

    public void setExpandedSubtitleTextColor(int expandedSubtitleTextColor) {
        this.expandedSubtitleTextColor = expandedSubtitleTextColor;
        onDependentViewChanged(parent, child, dependency);
    }
}
