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

    render: function (data, component, templateName) {
        var template = "";
        console.log('template: ',template);
        console.log('-');
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
        ///
        setTimeout(function(){
          $('#modal_loading_page').modal('hide'); 
          $('#modal_loading').modal('hide');
        }, 1000);
        ///
    }

};