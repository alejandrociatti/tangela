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
            jsRoutes.controllers.Startups.getUsersInfoByCriteria(locationId, market, undefined, date).ajax({
                method: 'GET',
                responseType: 'application/json',
                success: successHandler,
                error: errorHandler
            });
        },
        getStartupsByFeatures: function(locationId, date, market, quality, successHandler, errorHandler){
            jsRoutes.controllers.Startups.startupCriteriaSearch(locationId, market, undefined, date).ajax({
                method:'GET',
                responseType: 'application/json',
                success: successHandler,
                error: errorHandler
            });
        },
        getStartupsNetwork: function(locationId, date, market, quality, successHandler, errorHandler){
            jsRoutes.controllers.Networks.getStartupsNetwork(locationId, market, undefined, date).ajax({
                method:'GET',
                responseType: 'application/json',
                success: successHandler,
                error: errorHandler
            });
        },
        getPeopleNetwork: function(locationId, date, market, quality, successHandler, errorHandler){
            jsRoutes.controllers.Networks.getPeopleNetwork(locationId, market, undefined, date).ajax({
                method:'GET',
                responseType: 'application/json',
                success: successHandler,
                error: errorHandler
            });
        },
        startupsFundingByCriteria: function(locationId, date, market, quality, successHandler, errorHandler){
            jsRoutes.controllers.Startups.startupsFundingByCriteria(locationId, market, undefined, date).ajax({
                method:'GET',
                responseType: 'application/json',
                success: successHandler,
                error: errorHandler
            });
        },
        getCSV: function(json, successHandler, errorHandler){
            jsRoutes.controllers.Application.tableToCSV().ajax({
                method: 'POST',
                contentType: 'text/json',
                data: json,
                success: successHandler,
                error: errorHandler
            })
        },
        getStartupsNetworkCSV: function(locationId, date, market, quality, successHandler, errorHandler){
            jsRoutes.controllers.CSVs.getStartupsNetworkCSV(locationId, market, undefined, date).ajax({
                method:'GET',
                responseType: 'application/json',
                success: successHandler,
                error: errorHandler
            })
        },
        getStartupsCSV: function(locationId, date, market, quality, successHandler, errorHandler){
            jsRoutes.controllers.CSVs.getStartupsCSV(locationId, market, undefined, date).ajax({
                method:'GET',
                responseType: 'application/json',
                success: successHandler,
                error: errorHandler
            })
        },
        getUsersCSV: function(locationId, date, market, quality, successHandler, errorHandler){
            jsRoutes.controllers.CSVs.getUsersCSV(locationId, market, undefined, date).ajax({
                method:'GET',
                responseType: 'application/json',
                success: successHandler,
                error: errorHandler
            })
        },
        getStartupsTagsCSV: function(locationId, date, market, quality, successHandler, errorHandler){
            jsRoutes.controllers.CSVs.getStartupsTagsCSV(locationId, market, undefined, date).ajax({
                method:'GET',
                responseType: 'application/json',
                success: successHandler,
                error: errorHandler
            })
        },
        getPeopleNetworkCSV: function(locationId, date, market, quality, successHandler, errorHandler){
            jsRoutes.controllers.CSVs.getPeopleNetworkCSV(locationId, market, undefined, date).ajax({
                method:'GET',
                responseType: 'application/json',
                success: successHandler,
                error: errorHandler
            });
        },
        getStartupRolesCSV: function(startupId, successHandler, errorHandler){
            jsRoutes.controllers.CSVs.getStartupRolesCSV(startupId).ajax({
                method:'GET',
                responseType: 'application/json',
                success: successHandler,
                error: errorHandler
            });
        },
        getStartupFundingCSV: function(startupId, successHandler, errorHandler){
            jsRoutes.controllers.CSVs.getStartupFundingCSV(startupId).ajax({
                method:'GET',
                responseType: 'application/json',
                success: successHandler,
                error: errorHandler
            });
        },
        getStartupsFundingsCSV: function(locationId, date, market, quality, successHandler, errorHandler){
            jsRoutes.controllers.CSVs.getStartupsFundingsCSV(locationId, market, undefined, date).ajax({
                method:'GET',
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