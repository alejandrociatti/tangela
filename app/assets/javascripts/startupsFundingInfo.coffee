angular.module 'AAC', ['app.controllers']

module = angular.module 'app.controllers', ['app.services']

module.controller 'startupsFundingInfo', ['$scope', 'dataAccess', ($scope, dataAccess) ->
  dateFromHolder = $('#creation-date-from')
  dateToHolder = $('#creation-date-to')
  criteriaObject = {}
  $scope.startupsResultsReached = true
  $scope.searching = false
  $scope.optionSelectMsg = 'Search first.'
  $scope.persons = []
  $scope.fundings = []
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
    criteriaObject = {}
    criteriaObject.location = if $scope.deepLocation then $scope.deepLocation else $scope.location
    criteriaObject.market = $scope.market
    dateFrom = dateFromHolder.val()
    dateTo = dateToHolder.val()
    criteriaObject.date = "(#{dateFrom},#{dateTo})" if dateFrom || dateTo
    criteriaObject.quality = "(#{$scope.qualityFrom},#{$scope.qualityTo})" if $scope.qualityFrom || $scope.qualityTo

    dataAccess.startupsFundingByCriteria criteriaObject, (fundings) ->
      $scope.$apply ->
        $scope.fundings = sortByKeys(fundings, "name")
        $scope.searching = false
        $scope.optionSelectMsg = 'Select a startup.'
        $scope.exportStartupsFundingCSVURL = dataAccess.getStartupsFundingsCSVURL(criteriaObject)

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

  $scope.pageCount =  -> Math.ceil($scope.fundings.length/$scope.itemsPerPage)-1

  # Result sorting helpers
  sortByKeys = (array, key1) -> array.sort((a,b) -> compareByKey(a,b,key1))

  compareByKey = (o1, o2, key) -> x = o1[key]; y = o1[key]; if x<y then -1 else if x>y then 1 else 0
]

module.filter 'offset', ->
  (input, start) ->
    start = parseInt(start, 10)
    (input || []).slice(start)