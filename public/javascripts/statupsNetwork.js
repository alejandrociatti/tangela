/**
 * Created by Joaquin on 17/09/2014.
 */


angular.module('JB3', ['app.controllers']);

var module = angular.module('app.controllers', ['app.services']);

module.controller('startupsNetworkCtrl', ['$scope', 'dataAccess',
        function ($scope, dataAccess) {

            $scope.startupsResultsReached= true;
            $scope.searching= false;
            $scope.optionSelectMsg = 'Search first.';
            $scope.persons= [] ;

            $scope.searchForStartupsNetwork= function () {
                $scope.optionSelectMsg = 'Loading results...';
                $scope.startupsResultsReached= true;
                $scope.searching= true;
                dataAccess.getStartupsNetwork($scope.location, $scope.date, $scope.market, -1, function(startups){
                    $scope.startups= startups;
                    $scope.searching= false;
                    $scope.startupsResultsReached= startups.length != 0;
                    $scope.optionSelectMsg = 'Select a startup.';
                    $scope.$apply();
                });
            };

            $scope.export= function () {


                var obj = {
                    headers: ["Startup Id One", "Startup Name One","User Role in Startup One","Startup Id Two"
                        ,"Startup Name Two", "User Role in Startup Two","User in common Id","User in common Name"],
                    values: []
                } ;
                for (var i = 0; i < $scope.startups.length; i++) {
                    var startup = $scope.startups[i];
                    obj.values.push([startup.startupIdOne,startup.startupNameOne,startup.roleOne,
                        startup.startupIdTwo,startup.startupNameTwo,startup.roleTwo,startup.userId,startup.userName]);
                }

                dataAccess.getCSVStartupsNetwork(JSON.stringify(obj), function(file){
                    console.log(file);
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
                return Math.ceil($scope.persons.length/$scope.itemsPerPage)-1;
            };


        }]
);

module.filter('offset', function() {
    return function(input, start) {
        start = parseInt(start, 10);
        /**
         * Changed input.slice(start); in case input is undefined.
         * This 'offset' function is used on ng-repeat directives,
         * to change the starting element of the repeat.
         * In this startupsInfo, it is used on $scope.roles, which for some reason was undefined.
         * TODO: Check why typeof($scope.roles) == undefined at any given moment.
         */
        return (input || []).slice(start);
    };
});