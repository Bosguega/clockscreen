package br.com.clockscreen

import br.com.clockscreen.R

/**
 * Variante de painel digital. Compartilha o comportamento do widget Glass,
 * alterando apenas o layout usado pelo RemoteViews.
 */
class DigitalPanelWidgetProvider : ClockWidgetProvider() {
    override val widgetLayoutResId: Int = R.layout.widget_clock_panel
}