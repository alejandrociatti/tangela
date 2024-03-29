/**
 * Created with IntelliJ IDEA.
 * User: alejandro
 * Date: 26/06/14
 * Time: 13:12
 */

angular.module('JB', ['app.controllers']);

var module = angular.module('app.controllers', ['app.services']);


module.controller('startupInfoCtrl', ['$scope', 'dataAccess',
        function ($scope, dataAccess) {
            $scope.roles = [];
            $scope.rounds = [];
            $scope.responseStatus= true;
            $scope.optionSelectMsg = 'Search first.';

            $scope.searchForStartupsByName = function () {
                $scope.optionSelectMsg = 'Loading results...';
                dataAccess.getStartupsByName($scope.startupName, function(startupsByName){
                    $scope.startupsByName= startupsByName;
                    $scope.responseStatus= startupsByName.length != 0;
                    $scope.optionSelectMsg = 'Select a startup.';
                    $scope.$apply();
                })
            };

            $scope.loadInfo = function(){
                $scope.searchNumberOfFounders();
                $scope.searchRolesOfStartup();
                $scope.searchStartupFunding();
            };

            $scope.searchStartupFunding = function(){
                dataAccess.getStartupFunding($scope.startupId, function(fundraising){
                    $scope.rounds = fundraising;
                    $scope.totalFunding = 0;
                    $scope.numberOfRounds = fundraising.length;
                    $scope.exportStartupFundingCSVURL = dataAccess.getStartupFundingCSVURL($scope.startupId);

                    console.log(fundraising);

                    for(var i = 0; i < fundraising.length; i++) {
                        $scope.rounds[i].participants = JSON.parse(fundraising[i].participants);
                        $scope.totalFunding += parseInt(fundraising[i].amount);
                        if(fundraising[i].round_type == ""){
                            $scope.rounds[i].round_type = "Doesn't have a type assigned";
                        }else {
                            $scope.rounds[i].round_type = fundraising[i].round_type;
                        }
                    }
                    $scope.$apply();
                })
            };

            $scope.searchNumberOfFounders= function(){
                dataAccess.getNumberOfFoundersByStartupId($scope.startupId, function(number){
                    $scope.numberOfFounders= number;
                    $scope.$apply();
                })
            };

            $scope.searchRolesOfStartup= function(){
                dataAccess.getRolesNetInfo($scope.startupId, function(persons){
                    $scope.exportStartupRolesCSVURL = dataAccess.getStartupRolesCSVURL($scope.startupId);
                    $scope.roles= persons;
                    $scope.$apply();
                })
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
                return Math.ceil($scope.roles.length/$scope.itemsPerPage)-1;
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