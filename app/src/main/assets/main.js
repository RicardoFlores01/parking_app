/* VISTA Y MAPA */
    const view = new ol.View({
        projection: 'EPSG:3857',
        center: ol.proj.fromLonLat([-102.552784, 23.634501]),
        zoom: 5,
        rotation: 0,
        enableRotation: false,
    });

    const map = new ol.Map({
        target: 'map',
        controls: [],
        view: view,
    });

/* CAPAS BASE */
    const osmLayer = new ol.layer.Tile({
        source: new ol.source.OSM(),
        visible: false,
        title: 'OSM',
    });
    map.addLayer(osmLayer);

    const bingimg = new ol.layer.Tile({
        source: new ol.source.BingMaps({
            key: 'AmX1FVgGILJ-v3nO2tttFP5-CrYrAIip7w0Uzd9T_UfQqZz6ZoDrmgyv2rnhxO9z',
            imagerySet: 'road',
        }),
        visible: true,
        title: '12-BING',
    });
    map.addLayer(bingimg);

/* FUENTES Y CAPAS VECTORIALES */
    const markersSource = new ol.source.Vector();
    const markersLayer = new ol.layer.Vector({
        source: markersSource,
    });
    map.addLayer(markersLayer);

    let routeSource = new ol.source.Vector(); // Ahora declarada globalmente
    let routeLayer = null; // Capa de ruta global para poder limpiarla

/* MARCADORES */
    let marker = null;
    let markerParking = null;
    let markerUsuario = null;
    let rutaActualInterval = null;

    function addMarker(lat, lon) {
        if (marker) markersSource.removeFeature(marker);

        marker = new ol.Feature({
            geometry: new ol.geom.Point(ol.proj.fromLonLat([lon, lat]))
        });

        marker.setStyle(new ol.style.Style({
            image: new ol.style.Icon({
                src: 'resources/images/location.png',
                scale: 0.11
            })
        }));

        markersSource.addFeature(marker);
        map.getView().setCenter(ol.proj.fromLonLat([lon, lat]));
        map.getView().setZoom(17);
    }

    function addMarkerParking(lat, lon) {
        // Eliminar el marcador con imagen location.png (marker)
        if (marker) {
            markersSource.removeFeature(marker);
            marker = null;  // importante limpiar la variable
        }

        // Luego agregas el marcadorParking
        if (markerParking) {
            markersSource.removeFeature(markerParking);
        }

        markerParking = new ol.Feature({
            geometry: new ol.geom.Point(ol.proj.fromLonLat([lon, lat]))
        });

        markerParking.setStyle(new ol.style.Style({
            image: new ol.style.Icon({
                src: 'resources/images/auto.png',
                scale: 0.10
            })
        }));

        markersSource.addFeature(markerParking);
        map.getView().setCenter(ol.proj.fromLonLat([lon, lat]));
        map.getView().setZoom(17);


        //localStorage.setItem('parkingLocation', JSON.stringify({ lat, lon }));

        if (typeof Android !== 'undefined') {
            Android.hideBtnParking();
        }
    }


/* RUTA ACTUAL (GEOPOSICIÓN) */
    function RutaActual() {
        if (!navigator.geolocation) {
            console.error('La geolocalización no está disponible.');
            return;
        }

        navigator.geolocation.getCurrentPosition((position) => {
            const { latitude, longitude } = position.coords;
            const coord = ol.proj.fromLonLat([longitude, latitude]);

            if (!markerUsuario) {
                markerUsuario = new ol.Feature({
                    geometry: new ol.geom.Point(coord)
                });

                markerUsuario.setStyle(new ol.style.Style({
                    image: new ol.style.Circle({
                        radius: 7,
                        fill: new ol.style.Fill({ color: 'rgba(33, 150, 243, 0.6)' }), // Azul translúcido
                        stroke: new ol.style.Stroke({ color: '#2196F3', width: 2 })     // Azul fuerte
                    })
                }));

                markersSource.addFeature(markerUsuario);
            } else {
                markerUsuario.getGeometry().setCoordinates(coord);
            }

        }, (error) => {
            console.error('Error al obtener la ubicación:', error);
        });
    }


/* CALCULAR RUTA */
    function calcularRuta() {
        const apiKey = '5b3ce3597851110001cf624894ea57ef6160486f8614b9d5e79bb828';

        if (!marker || !markerParking) {
            console.error('Faltan puntos para calcular la ruta.');
            return;
        }

        const start = ol.proj.toLonLat(marker.getGeometry().getCoordinates());         
        const end = ol.proj.toLonLat(markerParking.getGeometry().getCoordinates());    

        // Actualiza posición usuario
        navigator.geolocation.getCurrentPosition((position) => {
            const { latitude, longitude } = position.coords;
            const coord = ol.proj.fromLonLat([longitude, latitude]);

            if (!markerUsuario) {
                markerUsuario = new ol.Feature({
                    geometry: new ol.geom.Point(coord)
                });
                markerUsuario.setStyle(new ol.style.Style({
                    image: new ol.style.Circle({
                        radius: 7,
                        fill: new ol.style.Fill({ color: 'rgba(33, 150, 243, 0.6)' }),
                        stroke: new ol.style.Stroke({ color: '#2196F3', width: 2 })
                    })
                }));
                markersSource.addFeature(markerUsuario);
            } else {
                markerUsuario.getGeometry().setCoordinates(coord);
            }
        }, (error) => {
            console.error('Error al obtener la ubicación:', error);
        });

        // Remueve ruta anterior
        if (routeLayer) {
            map.removeLayer(routeLayer);
            routeLayer = null;
            routeSource.clear();
        }

        fetch('https://api.openrouteservice.org/v2/directions/foot-walking/geojson', {
            method: 'POST',
            headers: {
                'Authorization': apiKey,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ coordinates: [end, start] })
        })
        .then(response => response.json())
        .then(data => {
            const routeFeatures = new ol.format.GeoJSON().readFeatures(data, {
                dataProjection: 'EPSG:4326',
                featureProjection: 'EPSG:3857'
            });

            const summary = data.features[0].properties.summary;
            const distanciaKm = (summary.distance / 1000).toFixed(2); // en kilómetros
            const duracionMin = (summary.duration / 60).toFixed(1);   // en minutos

            if (typeof Android !== 'undefined') {
                Android.mostrarInfoRuta(distanciaKm, duracionMin);
            }


            routeSource = new ol.source.Vector({
                features: routeFeatures
            });

            routeLayer = new ol.layer.Vector({
                source: routeSource,
                style: new ol.style.Style({
                    stroke: new ol.style.Stroke({
                        color: '#4FC3F7',
                        width: 4
                    })
                })
            });

            map.addLayer(routeLayer);

            map.getView().fit(routeSource.getExtent(), {
                padding: [50, 50, 50, 50],
                maxZoom: 17
            });
        })
        .catch(error => {
            console.error('Error al obtener la ruta:', error);
        });

        if (typeof Android !== 'undefined') {
            Android.showCardInfo();
        }
    }


/* UTILIDADES */
    function navigateTo() {
        setTimeout(() => {
            console.log('Calculando ruta automáticamente...');
            calcularRuta();

            // Inicia la actualización periódica solo una vez
            if (rutaActualInterval) clearInterval(rutaActualInterval);
            rutaActualInterval = setInterval(calcularRuta, 15000);
        }, 2000);
    }


    function clearMap() {
        markersSource.clear();
        if (routeLayer) {
            map.removeLayer(routeLayer);
            routeLayer = null;
        }
        routeSource.clear();
        localStorage.removeItem('parkingLocation');
        console.log("Mapa limpio");
    }

    function zoomInMap() {
        const view = map.getView();
        view.setZoom(view.getZoom() + 1);
    }

    function zoomOutMap() {
        const view = map.getView();
        view.setZoom(view.getZoom() - 1);
    }
