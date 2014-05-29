/**
 * Created with IntelliJ IDEA.
 * User: alejandro
 * Date: 23/05/14
 * Time: 00:14
 */

var demoAppModule = angular.module('DemoApp', ['app.controllers']);

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

        $scope.searchNumberOfFounders= function(){
            console.log("holaaaa");
            dataAccess.getNumberOfFoundersByStartupId($scope.startupId, function(number){
                console.log(number);
                $scope.numberOfFounders= number;
                $scope.$apply();
            })
        };

        $scope.searchRolesOfStartup= function(){
            dataAccess.getRolesOfStartup($scope.startupId, function(persons){
                $scope.roles= persons;
                $scope.$apply();
            })
        };

        $scope.submit = function(){
            console.log($scope.area);
        }
    }]);

demoCtrlModule.controller('aacDemoCtrl', ['$scope', 'dataAccess',
    function ($scope, dataAccess) {

        $scope.loadChildren = function(){
            dataAccess.getChildrenOf($scope.country, function(data){
                console.log(data);
                $scope.children = data;
                $scope.$apply();
            },
            function(error){
                console.log(error);
            });
        };

        $scope.submit = function(){
            var locationId = $scope.country;
            if($scope.area){
                var i = $scope.children.length;
                while(i--){
                    if($scope.children[i].name === $scope.area){
                        locationId = $scope.children[i].id;
                        break;
                    }
                }
            }
            (locationId) && dataAccess.getStartupsByLocation(locationId, function(startups){
                $scope.startups = startups;
                $scope.$apply();
            });
        };

    }
]);

