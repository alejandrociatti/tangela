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

        $scope.showNetwork = function(startupId){
            var s = new sigma('network-container');
            var edgeId = 0;

            dataAccess.getStartupNetInfo(startupId, function(startup){
                s.graph.addNode({
                    id: 'n'+startupId,
                    label: startup.name,
                    size: startup.follower_count/10,
                    x: Math.random(),
                    y: Math.random(),
                    color: '#b32e2b'
                });
            });

            dataAccess.getRolesNetInfo(startupId, function(roles){
                var i = roles.length;
                while(i--){
                    s.graph.addNode({
                        id: 'n'+roles[i].id,
                        label: roles[i].name+' '+ roles[i].role,
                        size: roles[i].follower_count/100,
                        x: Math.random(),
                        y: Math.random(),
                        color:'#2bb372'
                    });
                    s.graph.addEdge({
                        id: 'e'+(edgeId++),
                        source: 'n'+startupId,
                        target: 'n'+roles[i].id,
                        color: '#2b6cb3'
                    });
                }
                s.startForceAtlas2();
                setTimeout(function(){
                    s.stopForceAtlas2();
                }, 3000);
            });

        };

    }
]);

