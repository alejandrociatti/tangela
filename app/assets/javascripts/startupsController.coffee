angular.module 'AAC', ['app.controllers', 'ui.bootstrap']

module = angular.module 'app.controllers', ['app.services']

module.controller 'startupsCtrl', ['$scope', 'dataAccess', ($scope, dataAccess) ->
  dateFromHolder = $('#creation-date-from')
  dateToHolder = $('#creation-date-to')
  criteriaObject = {}
  $scope.startupsResultsReached = true
  $scope.searching = false
  $scope.optionSelectMsg = 'Search first.'
  $scope.startups = []
  $scope.startupsToShow = []
  $scope.tags = []
  $scope.locations = []
  $scope.qualityFrom = ''
  $scope.qualityTo = ''

  # Locations loader function
  $scope.getLocations = ->
    dataAccess.getChildrenOf $scope.location, (children) ->
      $scope.$apply -> $scope.locations = children

  # Form submit function
  $scope.submit = ->
    $scope.optionSelectMsg = 'Loading results...'
    $scope.startupsResultsReached = true
    $scope.searching = true
    criteriaObject.location = if $scope.deepLocation then $scope.deepLocation else $scope.location
    criteriaObject.market = $scope.market
    dateFrom = dateFromHolder.val()
    dateTo = dateToHolder.val()
    criteriaObject.date = "(#{dateFrom},#{dateTo})" if dateFrom || dateTo
    criteriaObject.quality = "(#{$scope.qualityFrom},#{$scope.qualityTo})" if $scope.qualityFrom || $scope.qualityTo

    dataAccess.getStartupsAndTagsByCriteria criteriaObject, (response) ->
      $scope.$apply ->
        $scope.startups = response.startups
        $scope.exportStartupsURL = dataAccess.getStartupsCSVURL(criteriaObject)
        $scope.tags = response.tags
        $scope.exportStartupsTagsURL = dataAccess.getStartupsTagsCSVURL(criteriaObject)
        $scope.searching = false
        $scope.startupsResultsReached = scope.startups.length != 0
        $scope.optionSelectMsg = 'Select a startup.'

  # Pagination control
  $scope.itemsPerPage = 5
  $scope.currentPage = 0

  $scope.range = ->
    rangeSize = 5
    range = []
    start = if $scope.currentPage > 0 then $scope.currentPage-1 else $scope.currentPage
    finish = if $scope.pageCount() < start+rangeSize-1 then $scope.pageCount() else start+rangeSize-1
    range.push(i) for i in [start..finish] when i>=0
    range

  $scope.prevPage = -> $scope.currentPage-- if $scope.currentPage>0
  $scope.nextPage = -> $scope.currentPage++ if $scope.currentPage<$scope.pageCount()
  $scope.setPage = (n) -> $scope.currentPage = n

  $scope.nextPageDisabled = -> if $scope.currentPage >= $scope.pageCount() then "disabled" else ""
  $scope.prevPageDisabled = -> if $scope.currentPage == 0 then "disabled" else ""

  $scope.pageCount =  -> Math.ceil($scope.startups.length/$scope.itemsPerPage)-1
]

module.filter 'offset', ->
  (input, start) ->
    start = parseInt(start, 10)
    (input || []).slice(start)