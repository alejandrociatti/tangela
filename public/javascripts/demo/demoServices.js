/**
 * Created with IntelliJ IDEA.
 * User: alejandro
 * Date: 23/05/14
 * Time: 00:09
 */

angular.module('app.services', []).factory('locationAccess', function() {
    return {
        getChildrenOf: function(countryId, successHandler, errorHandler) {
            jsRoutes.controllers.Locations.getChildrenOf(countryId).ajax({
                method:'GET',
                responseType:"application/json",
                success: successHandler,
                error: errorHandler
            });
        }
    };
});