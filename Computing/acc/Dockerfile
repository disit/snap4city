FROM rstudio/plumber

# Install R packages
RUN R -e "install.packages('jsonlite')"
RUN R -e "install.packages('httr')"
RUN R -e "install.packages('tictoc')"
RUN R -e "install.packages('stringr')"
RUN R -e "install.packages('RCurl')"
RUN R -e "install.packages('varhandle')"
RUN R -e "install.packages('sgeostat')"
RUN R -e "install.packages('geosphere')"
RUN R -e "install.packages('rlist')"

# Set working directory
WORKDIR /api

# Copy plumber API file (assumed to be plumber.R)
COPY *.R /api/

# Expose API port
EXPOSE 8000

# Command to run the Plumber API
ENTRYPOINT ["R", "-e", "pr <- plumber::plumb('acc_service1_2_2.R'); pr$run(host='0.0.0.0', port=8000)"]
##CMD ["/api/acc_service1_2_2.R"]
