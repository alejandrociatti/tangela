/**
 * Created with IntelliJ IDEA.
 * User: alejandro
 * Date: 23/05/14
 * Time: 00:09
 */
var serviceModule = angular.module('app.services', []);


serviceModule.factory('dataAccess', function() {
    return {
        getChildrenOf: function(countryId, successHandler, errorHandler) {
            jsRoutes.controllers.Locations.getChildrenOf(countryId).ajax({
                method:'GET',
                responseType:"application/json",
                success: successHandler,
                error: errorHandler
            });
        },
        getLocationsByName: function(locationName, successHandler, errorHandler) {
            jsRoutes.controllers.Locations.getCountriesByString(encodeURIComponent(locationName)).ajax({
                method: 'GET',
                responseType:"application/json",
                success: successHandler,
                error: errorHandler
            })
        },
        getStartupsByLocation: function(locationId, successHandler, errorHandler) {
            jsRoutes.controllers.Startups.getStartupsByLocationId(locationId).ajax({
                method: 'GET',
                responseType:"application/json",
                success: successHandler,
                error: errorHandler
            })
        },
        getStartupNetInfo: function(startupId, successHandler, errorHandler){
            jsRoutes.controllers.Startups.getStartupNetInfo(startupId).ajax({
                method: 'GET',
                responseType: "application/json",
                success: successHandler,
                error: errorHandler
            });
        },
        getRolesNetInfo: function(startupId, successHandler, errorHandler) {
            jsRoutes.controllers.Startups.getRolesOfStartup(startupId).ajax({
                method: 'GET',
                responseType: "application/json",
                success: successHandler,
                error: errorHandler
            });
        },
        getNumberOfFoundersByStartupId: function(startupId, successHandler, errorHandler) {
            jsRoutes.controllers.Startups.getNumberOfFoundersByStartupId(startupId).ajax({
                method: 'GET',
                responseType: "application/json",
                success: successHandler,
                error: errorHandler
            });
        },
        getStartupsByName: function(startupName, successHandler, errorHandler) {
            jsRoutes.controllers.Startups.getStartupsByName(startupName).ajax({
                method: 'GET',
                responseType: "application/json",
                success: successHandler,
                error: errorHandler
            });
        },
        getStartupFunding: function(startupId, successHandler, errorHandler){
            jsRoutes.controllers.Startups.getStartupFunding(startupId).ajax({
                method: 'GET',
                responseType: 'application/json',
                success: successHandler,
                error: errorHandler
            });
        },
        getStartupPeopleInfo: function(startupId, successHandler, errorHandler){
            jsRoutes.controllers.Startups.getAllInfoOfPeopleInStartups(startupId).ajax({
                method: 'GET',
                responseType: 'application/json',
                success: successHandler,
                error: errorHandler
            });
        }
    };
});

serviceModule.factory('graphUtil', function() {
    var _scales = [
        {threshold: 100, base: 10, scale: 0.1},
        {threshold: 200, base: 20, scale: 0.1},
        {threshold: 500, base: 50, scale: 0.1},
        {threshold: 1500, base: 100, scale: 0.1},
        {threshold: undefined, base: 300, scale: 0.01}
    ];

    return {
        getRoleSize: function (sizeVariable) {
            for(var i = 0; i < _scales.length; i++){
                if(sizeVariable < _scales[i].threshold || _scales[i].threshold === undefined){
                    return  _scales[i].base+sizeVariable*_scales[i].scale;
                }
            }
        },
        getStartupSize: function(sizeVariable){
            var size;
            for(var i = 0; i < _scales.length; i++){
                if(sizeVariable < _scales[i].threshold || _scales[i].threshold === undefined){
                    size=  _scales[i].base+sizeVariable*_scales[i].scale; break;
                }
            }
            size += size*0.5;
            return size;
        }
    };
});