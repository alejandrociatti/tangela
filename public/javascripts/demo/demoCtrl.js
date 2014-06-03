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

demoCtrlModule.controller('startupInfoCtrl', ['$scope', 'dataAccess',
    function ($scope, dataAccess) {

        $scope.searchNumberOfFounders= function(){
            dataAccess.getNumberOfFoundersByStartupId($scope.startupId, function(number){
                $scope.numberOfFounders= number;
                $scope.$apply();
            })
        };

        $scope.searchRolesOfStartup= function(){
            dataAccess.getRolesNetInfo($scope.startupId, function(persons){
                $scope.roles= persons;
                $scope.$apply();
            })
        };
    }]
);

demoCtrlModule.controller('aacDemoCtrl', ['$scope', 'dataAccess', 'graphUtil',
    function ($scope, dataAccess, graphUtil) {
        $scope.childrenSelectMsg = 'Select a country first.';
        $scope.children = [];

        $scope.loadChildren = function(){
            $scope.children = [];
            $scope.childrenSelectMsg = 'Loading areas...';
            dataAccess.getChildrenOf($scope.country, function(data){
                console.log(data);
                $scope.children = data;
                $scope.childrenSelectMsg = 'Select area...';
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
            $('#network-container').empty();
            var s = new sigma('network-container');
            var edgeId = 0;

            dataAccess.getStartupNetInfo(startupId, function(startup){
                while(startup.follower_count > 100) startup.follower_count = startup.follower_count/10;
                s.graph.addNode({
                    id: 'ns'+startupId,
                    label: startup.name,
                    size: graphUtil.getStartupSize(startup.follower_count),
                    x: Math.random(),
                    y: Math.random(),
                    color: '#b32e2b'
                });

                dataAccess.getRolesNetInfo(startupId, function(roles){
                    var i = roles.length;
                    while(i--){
                        s.graph.addNode({                               //.dropNode('nr'+roles[i].id)
                            id: 'nr'+roles[i].id+roles[i].role,
                            label: roles[i].name+' '+ roles[i].role,
                            size: graphUtil.getRoleSize(roles[i].follower_count),
                            x: Math.random(),
                            y: Math.random(),
                            color:'#2bb372'
                        });
                        s.graph.addEdge({
                            id: 'e'+(edgeId++),
                            source: 'ns'+startupId,
                            target: 'nr'+roles[i].id+roles[i].role,
                            color: '#2b6cb3'
                        });
                    }
                    s.startForceAtlas2();
                    setTimeout(function(){
                        s.stopForceAtlas2();
                    }, 3000);
                    console.log(s.graph.nodes())
                });

            });
        };

        //Pagination control:
        $scope.itemsPerPage = 5;
        $scope.currentPage = 0;

        $scope.range = function() {
            var rangeSize = 5;
            var ret = [];
            var start;

            start = $scope.currentPage;
            if ( start > $scope.pageCount()-rangeSize ) {
                start = $scope.pageCount()-rangeSize+1;
            }

            for (var i=start; i<start+rangeSize; i++) {
                ret.push(i);
            }
            return ret;
        };

        $scope.prevPage = function() {
            if ($scope.currentPage > 0) {
                $scope.currentPage--;
            }
        };

        $scope.setPage = function(n) {
            $scope.currentPage = n;
        };

        $scope.nextPage = function() {
            if ($scope.currentPage < $scope.pageCount()) {
                $scope.currentPage++;
            }
        };

        $scope.prevPage = function() {
            if ($scope.currentPage > 0) {
                $scope.currentPage--;
            }
        };

        $scope.nextPageDisabled = function() {
            return $scope.currentPage === $scope.pageCount() ? "disabled" : "";
        };

        $scope.prevPageDisabled = function() {
            return $scope.currentPage === 0 ? "disabled" : "";
        };

        $scope.pageCount = function() {
            return Math.ceil($scope.startups.length/$scope.itemsPerPage)-1;
        };
    }
]);

demoCtrlModule.filter('offset', function() {
    return function(input, start) {
        start = parseInt(start, 10);
        return input.slice(start);
    };
});

