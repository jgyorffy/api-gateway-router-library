package com.datapark.agwy.lambda;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Router {
    private final List<Controller> listControllers = new ArrayList<>(20);
    private volatile boolean isLoaded;
    private Set<String> loadedControllers = new HashSet<>();
    private final RouteHandler routeHandler;
    Router(RouteHandler routeHandler) {
        this.routeHandler = routeHandler;
    }

    void loadControllers() {
        if (!isLoaded) {
            listControllers.stream().forEach(c -> c.loadRoutes(routeHandler));
        }
        isLoaded = true;
    }

    RouteHandler getRouteHandler() {
        return routeHandler;
    }

    public void addController(Controller controller) {
        if (!isLoaded && controller != null && !loadedControllers.contains(controller.getClass().getCanonicalName())) {
            listControllers.add(controller);
            loadedControllers.add(controller.getClass().getCanonicalName());
        }
    }


}
