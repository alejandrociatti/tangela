/**
 * Created by Joaquin on 17/09/2014.
 */


angular.module('JB3', ['app.controllers']);

var module = angular.module('app.controllers', ['app.services', 'ui.bootstrap']);

module.controller('startupsNetworkCtrl', ['$scope', 'dataAccess',
        function ($scope, dataAccess) {
            var scope = this;
            var lastReq;
            this.startupsResultsReached= true;
            this.searching= false;
            this.optionSelectMsg = 'Search first.';
            this.startups= [] ;
            this.startupsToShow= [] ;
            this.markOne = false;

            this.searchForStartupsNetwork = function () {
                this.optionSelectMsg = 'Loading results...';
                this.startupsResultsReached = true;
                this.searching = true;
                this.markOne = !(this.location || this.market);
                if(!this.markOne) {
                    dataAccess.getStartupsNetwork(this.location, $("#creation-date").val(), this.market, -1, function (response) {
                        var startups = JSON.parse(response);
                        //en startups to show tengo los startups que tengo q mostrar en otra tablita
                        scope.startupsToShow= (startups[1]);
                        scope.startups = (startups[0]);
                        scope.searching = false;
                        scope.startupsResultsReached = startups.length != 0;
                        scope.optionSelectMsg = 'Select a startup.';
                        lastReq = {loc:scope.location, creation:$("#creation-date").val(), market:scope.market};
                        $scope.$apply();
                    });
                }
            };

            this.exportCSV = function () {
                dataAccess.getStartupsNetworkCSV(lastReq.loc, lastReq.creation, lastReq.market, -1, function(file){
                    if(file.error){
                        console.log(file.error)
                    }else{
                        var pom = document.createElement('a');
                        pom.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(file));
                        pom.setAttribute('download', 'startupsNetwork.csv');
                        pom.click();
                    }
                });

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