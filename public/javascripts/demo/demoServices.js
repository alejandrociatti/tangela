/**
 * Created with IntelliJ IDEA.
 * User: alejandro
 * Date: 23/05/14
 * Time: 00:09
 */
var serviceModule = angular.module('app.services', []);

serviceModule.factory('dataAccess', ['$http', function($http) {
    return {
        getChildrenOf: function(locationId, successHandler, errorHandler) {
            jsRoutes.controllers.Locations.getChildrenOf(locationId).ajax({
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
        getStartupsByCriteria: function(criteriaObj, successHandler, errorHandler){
            jsRoutes.controllers.Startups.startupCriteriaSearch(
                criteriaObj.locationId, criteriaObj.marketId, criteriaObj.quality, criteriaObj.creationDate).ajax({
                    method: 'GET',
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
        getStartupPeopleInfo: function(locationId, date, market, quality, successHandler, errorHandler){
            jsRoutes.controllers.Startups.getUsersInfoByCriteria(locationId, market, quality, date).ajax({
                method: 'GET',
                responseType: 'application/json',
                success: successHandler,
                error: errorHandler
            });
        },
        getStartupsNetwork: function(criteriaObj, successHandler, errorHandler){
            jsRoutes.controllers.Networks.getStartupsNetwork(criteriaObj.location, criteriaObj.market, criteriaObj.quality, criteriaObj.date).ajax({
                method:'GET',
                responseType: 'application/json',
                success: successHandler,
                error: errorHandler
            });
        },
        getPeopleNetwork: function(criteriaObj, successHandler, errorHandler){
            jsRoutes.controllers.Networks.getPeopleNetwork(criteriaObj.location, criteriaObj.market, criteriaObj.quality, criteriaObj.date).ajax({
                method:'GET',
                responseType: 'application/json',
                success: successHandler,
                error: errorHandler
            });
        },
        startupsFundingByCriteria: function(criteriaObj, successHandler, errorHandler){
            jsRoutes.controllers.Startups.startupsFundingByCriteria(criteriaObj.location, criteriaObj.market, criteriaObj.quality, criteriaObj.date).ajax({
                method:'GET',
                responseType: 'application/json',
                success: successHandler,
                error: errorHandler
            });
        },
        getUsersCSVURL: function(locationId, date, market, quality){
            return jsRoutes.controllers.CSVs.getUsersCSV(locationId, market, quality, date).url;
        },
        getStartupRolesCSVURL: function(startupId){
            return jsRoutes.controllers.CSVs.getStartupRolesCSV(startupId).url;
        },
        getStartupFundingCSVURL: function(startupId){
            return jsRoutes.controllers.CSVs.getStartupFundingCSV(startupId).url;
        }
    };
}]);