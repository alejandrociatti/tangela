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
            var lastReq;
            $scope.roles = [];
            $scope.rounds = [];
            $scope.startupsResultsReached= true;
            $scope.optionSelectMsg = 'Search first.';

            $scope.searchForStartupsByName = function () {
                $scope.optionSelectMsg = 'Loading results...';
                dataAccess.getStartupsByName($scope.startupName, function(startupsByName){
                    $scope.startupsByName= startupsByName;
                    $scope.startupsResultsReached= startupsByName.length != 0;
                    $scope.optionSelectMsg = 'Select a startup.';
                    $scope.$apply();
                })
            };

            $scope.loadInfo = function(){
                lastReq = {startupId: $scope.startupId};
                $scope.searchNumberOfFounders();
                $scope.searchRolesOfStartup();
                $scope.searchStartupFunding();
            };

            $scope.searchStartupFunding = function(){
                dataAccess.getStartupFunding($scope.startupId, function(fundraising){
                    $scope.rounds = fundraising;
                    $scope.totalFunding = 0;
                    $scope.numberOfRounds = fundraising.length;

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

            $scope.exportStartupRoles = function(){
                if(lastReq) {
                    dataAccess.getStartupRolesCSV(lastReq.startupId, function (file) {
                        if (file.error) {
                            console.log(file.error)
                        } else {
                            var pom = document.createElement('a');
                            pom.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(file));
                            pom.setAttribute('download', 'startups-net-' + lastReq.startupId + '.csv');
                            pom.click();
                        }
                    });
                }
            };

            $scope.exportStartupFunding = function(){
                var rounds = {
                    headers: ["Round Id", "Type", "Raised", "Closed at"],
                    values: []
                } ;

                var participants = {
                    headers: ["Round Id", "Participant Id", "Name", "Type"],
                    values: []
                } ;

                for (var i = 0; i < $scope.rounds.length; i++) {
                    var round = $scope.rounds[i];
                    rounds.values.push([round.id, round.round_type, round.amount, round.closed_at]);

                    var participants2 = round.participants;
                    for(var j = 0; j < participants2.length; j++){
                        var auxParticipant = participants2[j];
                        participants.values.push([round.id, auxParticipant.id, auxParticipant.name, auxParticipant.type])
                    }
                }

                dataAccess.getCSV(JSON.stringify(rounds),  function(file){
                    var pom = document.createElement('a');
                    pom.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(file));
                    pom.setAttribute('download', 'rounds.csv');
                    pom.click();
                });
                dataAccess.getCSV(JSON.stringify(participants), function(file){
                    var pom = document.createElement('a');
                    pom.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(file));
                    pom.setAttribute('download', 'participants.csv');
                    pom.click();
                });
            };

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


            //Pagination control:

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