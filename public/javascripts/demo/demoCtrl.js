/**
 * Created with IntelliJ IDEA.
 * User: alejandro
 * Date: 23/05/14
 * Time: 00:14
 */

angular.module('DemoApp', ['app.controllers']);

var demoCtrlModule = angular.module('app.controllers', ['app.services']);

demoCtrlModule.controller('sblDemoCtrl', ['$scope','dataAccess',
    function ($scope, dataAccess) {
        $scope.sent = false;

        $scope.searchLocation = function(){
            dataAccess.getLocationsByName($scope.locationName, function(locations){
                console.log(locations);
                $scope.locations = locations;
                $scope.$apply();
            })
        };

        $scope.searchStartups = function(){
            dataAccess.getStartupsByLocation($scope.locationId, function(startups){
                console.log(startups);
                $scope.startups = startups;
                $scope.$apply();
            })
        };


        $scope.submit = function(){
            console.log($scope.area);
        }
    }]);

demoCtrlModule.filter('offset', function() {
    return function(input, start) {
        start = parseInt(start, 10);
        return input.slice(start);
    };
});