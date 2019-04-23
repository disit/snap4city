var ViewManager = {

    loadTemplate: function (templateUrl) {
        var template = "";
        $.ajax({
            url: templateUrl,
            cache: false,
            async: false,
            dataType: "html",
            success: function (data) {
                template = data;
            }
        });
        return template;
    },

    renderTable: function (data, component, templateName) {
        var template = "";
        if (templateName == null) {
            for (var pageType in data) {
                template = this.loadTemplate("templates/" + pageType + ".mst.html");
                break;
            }
        } else {
            if (templateName.indexOf("mst.html") == -1) {
                template = this.loadTemplate("templates/" + templateName + ".mst.html");
            } else {
                template = this.loadTemplate(templateName);
            }
        }

        if (template != "") {
            html = Mustache.render(template, data);
        }

        $(component).empty();
        $(component).html(html);
    },

    render: function (component, templateName) {
        var template = "";
        if (templateName == null) {
            for (var pageType in data) {
                template = this.loadTemplate("templates/" + pageType + ".mst.html");
                break;
            }
        } else {
            if (templateName.indexOf("mst.html") == -1) {
                template = this.loadTemplate("templates/" + templateName + ".mst.html");
            } else {
                template = this.loadTemplate(templateName);
            }
        }

        if (template != "") {
        	$(component).empty();
            $(component).html(template);
        }
        else
        	$(component).empty();     
    }
    
};