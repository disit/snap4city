return {
    name = "check-validity",
    fields = {
        {
            config = {
                type = "record",
                fields = {
                    { checkme_url = { type = "string", default = "http://192.168.1.18:50000/checkme" } },
                },
            },
        },
    },
}

