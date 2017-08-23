package melerospaw.coordinator;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.support.annotation.ColorInt;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HeaderBehaviour extends CoordinatorLayout.Behavior<LinearLayout> {

    private static final float EXPANDED_BOTTOM_MARGIN = 32f;
    private static final float EXPANDED_LEFT_MARGIN = 32f;
    private static final float TITLE_EXPANDED_TEXT_SIZE = 30f;
    private static final float TITLE_COLLAPSED_TEXT_SIZE = 20f;

    private AppBarLayout appBarLayout;
    private Toolbar toolbar;
    private LinearLayout toolbarTitleContainer;
    private LinearLayout child;
    private TextView upperTitle;
    private TextView subtitle;

    private float expandedStartMargin;
    private float expandedBottomMargin;
    private float titleExpandedTextSize;
    private float titleCollapsedTextSize;
    private float subtitleExpandedTopMargin;
    private boolean subtitleExpandedTopMarginSpecified;
    private boolean interpolateTextColor;
    @ColorInt private int titleCollapsedTextColor;
    @ColorInt private int titleExpandedTextColor;
    @ColorInt private int subtitleCollapsedTextColor;
    @ColorInt private int subtitleExpandedTextColor;

    public HeaderBehaviour() {
        super();
    }

    public HeaderBehaviour(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.HeaderBehaviour);
        titleExpandedTextSize = attributes.getDimension(R.styleable.HeaderBehaviour_behaviour_titleMaxTextSize, -1);
        titleCollapsedTextSize = attributes.getDimension(R.styleable.HeaderBehaviour_behaviour_titleMinTextSize, -1);
        expandedStartMargin = attributes.getDimension(R.styleable.HeaderBehaviour_behaviour_expandedStartMargin, -1);
        expandedBottomMargin = attributes.getDimension(R.styleable.HeaderBehaviour_behaviour_expandedBottomMargin, -1);
        subtitleExpandedTopMargin = attributes.getDimension(R.styleable.HeaderBehaviour_behaviour_expandedSubtitleTopMargin, -1);
        @ColorInt int collapsedTextColor = attributes.getColor(R.styleable.HeaderBehaviour_behaviour_collapsedTextColor,
                ContextCompat.getColor(context, android.R.color.white));
        @ColorInt int expandedTextColor = attributes.getColor(R.styleable.HeaderBehaviour_behaviour_expandedTextColor,
                ContextCompat.getColor(context, android.R.color.black));
        titleCollapsedTextColor = attributes.getColor(R.styleable.HeaderBehaviour_behaviour_collapsedTitleColor, collapsedTextColor);
        titleExpandedTextColor = attributes.getColor(R.styleable.HeaderBehaviour_behaviour_expandedTitleColor, expandedTextColor);
        subtitleCollapsedTextColor = attributes.getColor(R.styleable.HeaderBehaviour_behaviour_collapsedSubitleColor, collapsedTextColor);
        subtitleExpandedTextColor = attributes.getColor(R.styleable.HeaderBehaviour_behaviour_expandedSubtitleColor, expandedTextColor);
        interpolateTextColor = attributes.getBoolean(R.styleable.HeaderBehaviour_behaviour_interpolateTextColor, true);

        titleExpandedTextSize = titleExpandedTextSize == -1 ?
                TITLE_EXPANDED_TEXT_SIZE : pxToDp(titleExpandedTextSize, context);
        titleCollapsedTextSize = titleCollapsedTextSize == -1 ?
                TITLE_COLLAPSED_TEXT_SIZE : pxToDp(titleCollapsedTextSize, context);
        expandedStartMargin = expandedStartMargin == -1 ?
                EXPANDED_LEFT_MARGIN : pxToDp(expandedStartMargin, context);
        expandedBottomMargin = expandedBottomMargin == -1 ?
                EXPANDED_BOTTOM_MARGIN : pxToDp(expandedBottomMargin, context);
        subtitleExpandedTopMarginSpecified = subtitleExpandedTopMargin != -1;

        attributes.recycle();
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, LinearLayout child, View dependency) {
        // En qué vista se tiene que fijar. Solo vale indicar vistas hijas directas del CoordinatorLayout
        return dependency instanceof AppBarLayout;
    }


    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, LinearLayout child, View dependency) {
        // Qué tienes que hacer cuando la vista cambies Annoi
        // Un behaviour solo se puede asignar a hijos directos del CoordinatorLayout.
        // El texto del título de la AppBar no cambia aunque se mueva con el CollapsingToolbarLayout
        // El CollapsingToolbarLayout tampoco se mueve nunca. Su tamaño siempre es extendido.

        obtainViews(dependency, child);
        changeTitleTextSize();
        changeSubtitleMarginTop();
        changeTitleColor();
        changePosition();
        toggleToolbarTitleVisibility();

        return true;
    }

    private void obtainViews(View dependency, LinearLayout child) {
        this.child = child;
        appBarLayout = (AppBarLayout) dependency;
        CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) appBarLayout.getChildAt(0);
        toolbar = (Toolbar) collapsingToolbarLayout.getChildAt(0);
        toolbarTitleContainer = (LinearLayout) toolbar.getChildAt(0);
        upperTitle = (TextView) child.getChildAt(0);
        subtitle = (TextView) child.getChildAt(1);
    }

    private void changeTitleTextSize() {
        // Averigua el rango de tamaño de letra entre el tamaño mayor y el menor
        float textSizeRange = titleExpandedTextSize - titleCollapsedTextSize;
        float textSizeDifference = textSizeRange * getYScrolledPercentage() / 100;
        float proportionedTextSize = titleCollapsedTextSize + textSizeDifference;
        upperTitle.setTextSize(proportionedTextSize);
    }

    private void changeSubtitleMarginTop() {
        if (subtitleExpandedTopMarginSpecified) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) subtitle.getLayoutParams();
            params.topMargin = (int) (getYScrolledPercentage() * subtitleExpandedTopMargin / 100);
        }
    }

    private void changeTitleColor() {
        if (interpolateTextColor) {
            @ColorInt int titleColor = (Integer) new ArgbEvaluator().evaluate(
                    getYScrolledPercentage() / 100, titleCollapsedTextColor, titleExpandedTextColor);
            @ColorInt int subtitleColor = (Integer) new ArgbEvaluator().evaluate(
                    getYScrolledPercentage() / 100, subtitleCollapsedTextColor, subtitleExpandedTextColor);
            upperTitle.setTextColor(titleColor);
            subtitle.setTextColor(subtitleColor);
        }
    }

    private void changePosition() {
        child.setY(getYPosition());
        child.setX(getXPosition());
    }

    private float getYPosition() {
        child.measure(View.MeasureSpec.UNSPECIFIED, child.getHeight());
        return appBarLayout.getBottom()
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
        float expandedXPosition = dpToPixels(expandedStartMargin, appBarLayout.getContext());
        float totalXScrollRange = collapsedXPosition - expandedXPosition;

        // Averigua el porcentaje de distancia vertical recorrido
        float yScrolledPercentage = getYScrolledPercentage();

        // Averigua y aplica la distancia horizontal que tiene que recorrer
        float xPorportionedRangeToBeScrolled = totalXScrollRange * yScrolledPercentage / 100;
        return collapsedXPosition - xPorportionedRangeToBeScrolled;
    }

    private float getYScrolledPercentage() {
        int yScrolledPixels = appBarLayout.getTotalScrollRange() + appBarLayout.getTop();
        return ((yScrolledPixels * 100) / (float) appBarLayout.getTotalScrollRange());
    }

    private static float dpToPixels(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    private void toggleToolbarTitleVisibility() {
        if (isToolbarCollapsed(appBarLayout)) {
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

    private static float pxToDp(float px, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return px / ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }
}
