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
            $scope.markOne = false;

            $scope.submit = function () {
                $scope.optionSelectMsg = 'Loading results...';
                $scope.startupsResultsReached= true;
                $scope.searching= true;
                $scope.markOne = !($scope.location || $scope.market);
                if(!$scope.markOne) {
                    dataAccess.getPeopleNetwork($scope.location, $('#creation-date').val(), $scope.market, $scope.quality, function (persons) {
                        $scope.persons = persons;
                        $scope.searching = false;
                        $scope.startupsResultsReached = persons.length != 0;
                        $scope.optionSelectMsg = 'Select a startup.';
                        $scope.$apply();
                    });
                }
            };

            $scope.export = function () {
                var obj = {
                    headers: ["User Id One", "User Name One","User Role One","User Id Two"
                        ,"User Name Two", "User Role Two","Startup in common Id","Startup in common Name"],
                    values: []
                } ;
                for (var i = 0; i < $scope.persons.length; i++) {
                    var person= $scope.persons[i];
                    obj.values.push([person.userIdOne,person.userNameOne,person.roleOne,
                        person.userIdTwo,person.userNameTwo,person.roleTwo,person.startupId,person.startupName]);
                }

                dataAccess.getCSV(JSON.stringify(obj), function(file){
                    var pom = document.createElement('a');
                    pom.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(file));
                    pom.setAttribute('download', 'peopleNetwork.csv');
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