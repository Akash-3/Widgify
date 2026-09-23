package com.widgify.desktop.widgets;

import javafx.scene.Node;
import java.util.Map;

public interface DesktopWidget {
    String getWidgetType();
    String getWidgetTitle();
    Node getWidgetNode();
    void onInitialize(Map<String, Object> config);
    void onClose();
}
