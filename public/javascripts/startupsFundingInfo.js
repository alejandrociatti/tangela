/**
 * Created with IntelliJ IDEA.
 * User: alejandro
 * Date: 18/09/14
 * Time: 17:17
 */

angular.module('AAC', ['app.controllers']);

var module = angular.module('app.controllers', ['app.services']);

module.controller('startupsFundingInfo', ['$scope', 'dataAccess',
        function ($scope, dataAccess) {

            var lastReq;
            var dateHolder = $("#creation-date");
            $scope.startupsResultsReached= true;
            $scope.searching= false;
            $scope.optionSelectMsg = 'Search first.';
            $scope.persons= [] ;
            $scope.fundings = [];

            $scope.submit = function () {
                $scope.optionSelectMsg = 'Loading results...';
                $scope.startupsResultsReached= true;
                $scope.searching= true;
                dataAccess.startupsFundingByCriteria($scope.location, $('#creation-date').val(), $scope.market, $scope.quality, function(fundings){
                    lastReq = {loc: $scope.location, creation: dateHolder.val(), market: $scope.market, quality: $scope.quality};
                    $scope.fundings = sortByKeys(fundings, "name");
                    $scope.searching = false;
                    $scope.startupsResultsReached= fundings.length != 0;
                    $scope.optionSelectMsg = 'Select a startup.';
                    $scope.exportStartupsFundingCSVURL = dataAccess.getStartupsFundingsCSVURL(
                        $scope.location, $scope.creation, $scope.market, $scope.quality
                    );
                    $scope.$apply();
                });
            };
            function sortByKeys(array, key1) {
                return array.sort(function(a, b) {
                    return compareByKey(a, b, key1);
                });
            }

            function compareByKey(obj1, obj2, key) {
                var x = obj1[key]; var y = obj2[key];
                return ((x < y) ? -1 : ((x > y) ? 1 : 0));
            }

            $scope.exportCSV = function () {
                if(lastReq) {
                    dataAccess.getStartupsFundingsCSV(lastReq.loc, lastReq.creation, lastReq.market, lastReq.quality, function (file) {
                        if (file.error) {
                            console.log(file.error)
                        } else {
                            var pom = document.createElement('a');
                            pom.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(file));
                            pom.setAttribute('download', 'startups-fundings-' + lastReq.loc + '.csv');
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
                return Math.ceil($scope.fundings.length/$scope.itemsPerPage)-1;
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