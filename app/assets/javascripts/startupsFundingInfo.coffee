angular.module 'AAC', ['app.controllers']

module = angular.module 'app.controllers', ['app.services', 'ui.bootstrap']

module.controller 'startupsFundingInfo', ['$scope', 'dataAccess', ($scope, dataAccess) ->
  dateFromHolder = $('#creation-date-from')
  dateToHolder = $('#creation-date-to')
  progressBar = $('#progress-bar')
  criteriaObject = {}
  $scope.responseStatus = true
  $scope.searching = false
  interval = undefined
  $scope.optionSelectMsg = 'Search first.'
  $scope.persons = []
  $scope.fundings = []
  $scope.locations = []
  $scope.qualityFrom = ''
  $scope.qualityTo = ''

  # Locations loader function
  $scope.getLocations = ->
    dataAccess.location.getChildren $scope.location, (children) ->
      $scope.locations = children

  # Form submit function
  $scope.submit = ->
    $scope.optionSelectMsg = 'Loading results...'
    $scope.responseStatus = true
    $scope.searching = true
    interval = startBar()
    criteriaObject = {}
    criteriaObject.location = if $scope.deepLocation then $scope.deepLocation else $scope.location
    criteriaObject.market = $scope.market
    dateFrom = dateFromHolder.val()
    dateTo = dateToHolder.val()
    criteriaObject.date = "(#{dateFrom},#{dateTo})" if dateFrom || dateTo
    criteriaObject.quality = "(#{$scope.qualityFrom},#{$scope.qualityTo})" if $scope.qualityFrom || $scope.qualityTo

    dataAccess.startup.getFundingsByCriteria criteriaObject, ((fundings) ->
      $scope.fundings = sortByKeys(fundings, "name")
      $scope.searching = false
      $scope.optionSelectMsg = 'Select a startup.'
      $scope.exportStartupsFundingCSVURL = dataAccess.csv.url.fundings(criteriaObject)
      stopBar()),                                                 # End success handler
      -> $scope.responseStatus = false           # Error handler

  # Progress bar functions
  intervalFn = -> progressBar.css 'width', (index, value) ->
    value = parseInt(value.substring(0, value.length-1), 10)+1
    progressBar.css('width', "#{value}%") if value <= 100

  startBar = -> setInterval intervalFn, 2000

  stopBar = -> clearInterval(interval); progressBar.css('width', '1%')

  # Result sorting helpers
  sortByKeys = (array, key1) -> array.sort((a,b) -> compareByKey(a,b,key1))

  compareByKey = (o1, o2, key) -> x = o1[key]; y = o1[key]; if x<y then -1 else if x>y then 1 else 0
]

# Pagination controller
module.controller 'tableController', ['$scope', ($scope) ->
  $scope.itemsPerPage = 6
  $scope.currentPage = 1

  $scope.setPage = (pageNo) -> $scope.currentPage = pageNo
]

# Offset filter
module.filter 'offset', ->
  (input, start) -> (input || []).slice(parseInt(start, 10))