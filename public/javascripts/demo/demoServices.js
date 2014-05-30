/**
 * Created with IntelliJ IDEA.
 * User: alejandro
 * Date: 23/05/14
 * Time: 00:09
 */

angular.module('app.services', []).factory('dataAccess', function() {
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
        }
    };
});