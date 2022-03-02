# BIMserver image

Instructions:
- Build image:
```
sudo docker build -t mybimserver:1.5.182 .
```

- Run container:
```
sudo docker run -it -v <local fodler path> : /usr/local/bimserver/home -p 8080:8080 --name bimserver <bimserver image name>

```
example:
```
sudo docker run -it --rm -v /home/bimserver_home:/usr/local/bimserver/home -p 8080:8080 --name bimserver mybimserver:1.5.182
```

