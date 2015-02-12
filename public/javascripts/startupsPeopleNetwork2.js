/**
 * Created by Joaquin on 17/09/2014.
 * Angularified by Alejandro on 12/11/2014.
 */

angular.module('AAC', ['app.controllers']);

var module = angular.module('app.controllers', ['app.services', 'ui.bootstrap']);

module.controller('startupsPplNetCtrl', ['$scope', 'dataAccess',
        function ($scope, dataAccess) {
            var scope = this;
            var dateHolder = $('#creation-date');
            this.startupsResultsReached= true;
            this.searching= false;
            this.optionSelectMsg = 'Search first.';
            this.people= [];
            this.startupsToShow= [];
            this.markOne = false;

            this.submit = function () {
                this.optionSelectMsg = 'Loading results...';
                this.startupsResultsReached= true;
                this.searching= true;
                this.markOne = !(this.location || this.market);
                if(!this.markOne) {
                    dataAccess.getPeopleNetwork(this.location, dateHolder.val(), this.market, this.quality, function (response) {
                        scope.startupsToShow = response.startups;
                        scope.people = response.rows;
                        scope.searching = false;
                        scope.startupsResultsReached = scope.people.length != 0;
                        scope.optionSelectMsg = 'Select a startup.';
                        scope.exportURL = dataAccess.getPeopleNetworkCSVURL(
                            scope.location, scope.creation, scope.market, scope.quality
                        );
                        $scope.$apply();
                    });
                }
            };
        }]
);

/**
 * Pagination control
 */
module.controller('tableController', ['$scope', function($scope){
    $scope.itemsPerPage = 6;
    $scope.currentPage = 1;

    $scope.setPage = function (pageNo) {
        $scope.currentPage = pageNo;
    };
}]);

/**
 * Offset filter
 */
module.filter('offset', function() {
    return function(input, start) {
        start = parseInt(start, 10);
        return (input || []).slice(start);
    };
});