/**
 * Created with IntelliJ IDEA.
 * User: alejandro
 * Date: 23/05/14
 * Time: 00:14
 */

var demoAppModule = angular.module('DemoApp', ['app.controllers']);

var demoCtrlModule = angular.module('app.controllers', ['app.services']);

demoCtrlModule.controller('demoCtrl', ['$scope',
    function ($scope, locationAccess) {
        $scope.sent = false;

        $scope.searchLocation = function(event){

            locationAccess.getLocationsByName($scope.locationName,function(){$scope.locations})
        };

        $scope.submit = function(){
            console.log($scope.area);
        }
    }]);

