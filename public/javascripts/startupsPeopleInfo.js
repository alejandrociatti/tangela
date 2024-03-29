/**
 * Created by Joaquin on 08/09/2014.
 */


angular.module('JB2', ['app.controllers']);

var module = angular.module('app.controllers', ['app.services']);

module.controller('startupPeopleInfoCtrl', ['$scope', 'dataAccess',
    function ($scope, dataAccess) {

        $scope.responseStatus= true;
        $scope.optionSelectMsg = 'Search first.';
        $scope.persons= [] ;
        var lastReq;
        var dateHolder = $("#creation-date");
        $scope.markOne= false;


        $scope.submit = function(){
            $scope.optionSelectMsg = 'Loading results...';
            $scope.responseStatus= true;
            $scope.markOne = !($scope.location || $scope.market);
            if(!$scope.markOne) {
                dataAccess.getStartupPeopleInfo($scope.location, dateHolder.val(), $scope.market, $scope.quality,  function (persons) {
                    console.log(persons);
                    $scope.persons = persons;
                    $scope.exportStartupPeopleInfoCSVURL = dataAccess.getUsersCSVURL($scope.location, $scope.creation, $scope.market, $scope.quality);
                    $scope.$apply();
                });
            }
        };


        $scope.exportCSV = function () {
            if(lastReq) {
                console.log(dataAccess)
                dataAccess.getUsersCSV(lastReq.loc, lastReq.creation, lastReq.market, lastReq.quality, function (file) {
                    if (file.error) {
                        console.log(file.error)
                    } else {
                        var pom = document.createElement('a');
                        pom.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(file));
                        pom.setAttribute('download', 'users-' + lastReq.loc + '.csv');
                        pom.click();
                    }
                });
            }
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