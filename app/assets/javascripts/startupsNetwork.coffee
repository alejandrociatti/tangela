angular.module 'JB3', ['app.controllers']

module = angular.module 'app.controllers', ['app.services', 'ui.bootstrap']

module.controller 'startupsNetworkCtrl', ['$scope', 'dataAccess', ($scope, dataAccess)->
  dateFromHolder = $('#creation-date-from')
  dateToHolder = $('#creation-date-to')
  progressBar = $('#progress-bar')
  criteriaObject = {}
  $scope.responseStatus = true
  $scope.searching = false
  interval = undefined
  $scope.optionSelectMsg = 'Search first.'
  $scope.startups = []
  $scope.networkRows = []
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
    dataAccess.startup.getNetwork criteriaObject, ((response) ->  # Pass criteriaObject & successHandler
      $scope.startups = response.startups
      $scope.networkRows = response.rows
      $scope.exportURL = dataAccess.csv.url.startupsNetwork(criteriaObject)
      $scope.searching = false
      $scope.responseStatus = response.startups.length != 0
      $scope.optionSelectMsg = 'Select a startup.'
      stopBar()),                                                 # End successHandler
      -> $scope.responseStatus = false           # Error handler

  # Progress bar functions
  intervalFn = -> progressBar.css 'width', (index, value) ->
    value = parseInt(value.substring(0, value.length-1), 10)+1
    progressBar.css('width', "#{value}%") if value <= 100

  startBar = -> setInterval intervalFn, 2000

  stopBar = -> clearInterval(interval); progressBar.css('width', '1%')
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