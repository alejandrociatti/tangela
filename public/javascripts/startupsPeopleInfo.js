/**
 * Created by Joaquin on 08/09/2014.
 */


angular.module('JB2', ['app.controllers']);

var module = angular.module('app.controllers', ['app.services']);

module.controller('startupPeopleInfoCtrl', ['$scope', 'dataAccess',
    function ($scope, dataAccess) {

        $scope.startupsResultsReached= true;
        $scope.optionSelectMsg = 'Search first.';
        $scope.persons= [] ;

        $scope.searchForStartupsByFeatures= function () {
            $scope.optionSelectMsg = 'Loading results...';
            $scope.startupsResultsReached= true;
            dataAccess.getStartupsByFeatures($scope.location, $("#creation-date").val(), $scope.market, -1, function(startupsByName){
                $scope.startupsByName= startupsByName;
                $scope.startupsResultsReached= startupsByName.length != 0;
                $scope.optionSelectMsg = 'Select a startup.';
                $scope.$apply();
            });
        };

        $scope.searchPeopleInfo= function(){
            dataAccess.getStartupPeopleInfo($scope.startupId, function(persons){
                console.log(persons);
                $scope.persons= persons;
                $scope.$apply();
            });
        };


        $scope.export= function () {


            var obj = {
                headers: ["Id","Name","Bio","Role","Followers","AngelList","Image","Blog","Online Bio","Twitter","Facebook",
                    "Linkedin","What He'd Built","What He Does","Investor"],
                values: []
            } ;
            for (var i = 0; i < $scope.persons.length; i++) {
                var person = $scope.persons[i];
                obj.values.push([person.id,person.name,person.bio,person.role,person.follower_count,person.angellist_url,
                    person.image,person.blog_url,person.online_bio_url,person.twitter_url,
                    person.facebook_url,person.linkedin_url,person.what_ive_built,person.what_i_do,person.investor]);
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