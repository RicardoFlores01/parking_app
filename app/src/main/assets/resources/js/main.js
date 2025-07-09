function init(){
	document.removeEventListener('DOMContentLoaded', init);

	var vectorLayer = new ol.layer.Vector({
		source: new ol.source.Vector({
			format: new ol.format.GeoJSON({
				defaultDataProjection: 'EPSG:4326'
			}),
			url: 'res/world_capitals.geojson',
			attributions: [
				new ol.Attribution({
					html: 'World Capitals'
				})
			]	
		}),
        // https://htmlcolorcodes.com/es/ de aqui se obtienen los colores, R,G,B,H
        style: new ol.style.Style({
            image: new ol.style.RegularShape({
                stroke: new ol.style.Stroke({
                    width: 2,
                    // borde
                    color: [52,113,157,205]
                }),
                fill: new ol.style.Fill({
                    // color de fondo
                    color: [150,211,255,205]
                }),
                points: 5,
                radius1: 5,
                radius2: 8,
                rotation: Math.PI
            })
        })
	});

	var map = new ol.Map({
		target: 'map',
		layers: [
			new ol.layer.Tile({
				source: new ol.source.OSM()
			}),
			vectorLayer
		],
		controls: [
			// default controls 
            new ol.control.Zoom(),
            new ol.control.Rotate(),
            new ol.control.Attribution(),
            // new controls 
            new ol.control.ZoomSlider(),
            new ol.control.MousePosition({
                coordinateFormat: function (coordinates) {
                    var coord_x = coordinates[0].toFixed(3);
                    var coord_y = coordinates[1].toFixed(3);
                    return coord_x + ', ' + coord_y;
                }
            }),
            new ol.control.ScaleLine(),
            new ol.control.OverviewMap(),

            new ol.control.ScaleLine({
                units: 'degrees'
            }),
            new ol.control.OverviewMap({
                collapsible: false
            })
		],
        interactions: ol.interaction.defaults().extend([
            new ol.interaction.Select({
                layers: [vectorLayer]
            })
        ]),
		
		view: new ol.View({
			center: [0,0],
			zoom: 2.8
		})
	});

}
document.addEventListener('DOMContentLoaded', init);