/**
 * Created by Joaquin on 17/09/2014.
 */


angular.module('AAC', ['app.controllers']);

var module = angular.module('app.controllers', ['app.services']);

module.controller('startupsPplNetCtrl', ['$scope', 'dataAccess',
        function ($scope, dataAccess) {

            $scope.startupsResultsReached= true;
            $scope.searching= false;
            $scope.optionSelectMsg = 'Search first.';
            $scope.persons= [] ;

            $scope.submit = function () {
                $scope.optionSelectMsg = 'Loading results...';
                $scope.startupsResultsReached= true;
                $scope.searching= true;
                dataAccess.getPeopleNetwork($scope.location, $('#creation-date').val(), $scope.market, $scope.quality, function(startups){
                    $scope.startups= startups;
                    $scope.searching= false;
                    $scope.startupsResultsReached= startups.length != 0;
                    $scope.optionSelectMsg = 'Select a startup.';
                    $scope.$apply();
                });
            };

            $scope.exportCSV = function () {
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

                dataAccess.getCSV(JSON.stringify(obj), function(file){
                    var pom = document.createElement('a');
                    pom.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(file));
                    pom.setAttribute('download', 'data.csv');
                    pom.click();
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