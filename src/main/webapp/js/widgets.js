// Widgify Central Widget Registry
(function() {
    const renderers = {};

    window.WidgetRegistry = {
        register: function(type, renderFn) {
            renderers[type.toLowerCase()] = renderFn;
        },
        get: function(type) {
            return renderers[type.toLowerCase()] || null;
        },
        has: function(type) {
            return !!renderers[type.toLowerCase()];
        }
    };

    console.log('Widgify WidgetRegistry initialized.');
})();
