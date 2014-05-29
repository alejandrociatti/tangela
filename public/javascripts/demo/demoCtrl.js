/**
 * Created with IntelliJ IDEA.
 * User: alejandro
 * Date: 23/05/14
 * Time: 00:14
 */

var demoAppModule = angular.module('DemoApp', ['app.controllers']);

var demoCtrlModule = angular.module('app.controllers', ['app.services']);

demoCtrlModule.controller('demoCtrl', ['$scope','locationAccess',
    function ($scope, locationAccess) {
        $scope.sent = false;

        $scope.searchLocation = function(){
            locationAccess.getLocationsByName($scope.locationName, function(locations){
                console.log(locations);
                $scope.locations = locations;
            })
        };

        $scope.searchStartups = function(){
            locationAccess.getStartupsByLocation($scope.locationId, function(startups){
                console.log(startups);
                $scope.startups = startups;
            })
        };

        $scope.submit = function(){
            console.log($scope.area);
        }
    }]);

