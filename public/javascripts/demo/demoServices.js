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
            //TODO: jsRoute al metodo de JoacOooOooOoOoO
            var data = {"id":6702,"name":"AngelList","follower_count":2849};
            successHandler(data);
        },
        getRolesNetInfo: function(startupId, successHandler, errorHandler){
            //TODO: jsRoute al metodo de Quitox
            var data = [{role: "employee",name: "Michael Daugherty", id: 190284,follower_count: 265},{role: "employee",name: "Kai Gradert",id: 17350,follower_count: 149}];
            successHandler(data);
        }
    };
});