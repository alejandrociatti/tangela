angular.module 'AAC', ['app.controllers', 'ui.bootstrap']

module = angular.module 'app.controllers', ['app.services']

module.controller 'searchCtrl', ['$scope', 'dataAccess',
  ($scope, dataAccess) ->
    $scope.submit = ->
      if($scope.locationId || $scope.marketId)
        dataAccess.getStartupsByCriteria(
          {
            locationId: $scope.locationId,
            marketId: $scope.marketId,
            quality: -1,
            creationDate: $('#creation-date').val(),
            quality: $scope.quality
          }
        ).success((data) -> console.log data)
  ]