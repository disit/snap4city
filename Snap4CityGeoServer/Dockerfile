FROM golang:1.14 AS builder
RUN apt update && apt-get install -y libgdal-dev
WORKDIR /go/src/heatmap2geoserver
COPY geoserver.* ./
COPY go.mod ./
ENV GO111MODULE=on
#RUN go env
#RUN go get github.com/lukeroth/gdal@56cda832b98a2f98e15aac59b38cfbf73995f69b
#RUN go mod download github.com/lukeroth/gdal@56cda832b98a2f98e15aac59b38cfbf73995f69b
RUN go get
RUN go build -o heatmap2geoserver geoserver.go

FROM debian:buster-slim
WORKDIR /heatmap2geoserver 
RUN apt update && apt-get install -y gdal-bin python-gdal python3-gdal 
COPY --from=builder /go/src/heatmap2geoserver/heatmap2geoserver .
CMD ./heatmap2geoserver

