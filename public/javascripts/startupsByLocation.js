/**
 * Created with IntelliJ IDEA.
 * User: alejandro
 * Date: 26/06/14
 * Time: 13:07
 */

angular.module('AAC', ['app.controllers', 'ui.bootstrap']);

var module = angular.module('app.controllers', ['app.services']);

module.controller('startupsCtrl', ['$scope', 'dataAccess', 'graphUtil',
    function ($scope, dataAccess, graphUtil) {
        $scope.startupsMsg = '';
        $scope.childrenSelectMsg = 'Select a country first.';
        $scope.startups = [];

        $scope.loadChildren = function(){
            $scope.children = [];
            $scope.childrenSelectMsg = 'Loading areas...';
            dataAccess.getChildrenOf($scope.country, function(data){
                    $scope.children = data;
                    $scope.childrenSelectMsg = 'Select area...';
                    $scope.$apply();
                },
                function(error){
                    console.log(error);
                });
        };

        $scope.submit = function(){
            $scope.startupsMsg = 'Loading startups...';
            var locationId;
            if($scope.area) locationId = $scope.area;
            else locationId = $scope.country;
            (locationId) && dataAccess.getStartupsByLocation(locationId, function(startups){
                $scope.startups = startups;
                if(startups.length > 0) $scope.startupsMsg = '';
                else $scope.startupsMsg = 'No startups were found...';
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
                if(i >= 0) {
                    ret.push(i);
                }
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

module.filter('offset', function() {
    return function(input, start) {
        start = parseInt(start, 10);
        return input.slice(start);
    };
});