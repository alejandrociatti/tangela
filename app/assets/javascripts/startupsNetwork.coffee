angular.module 'JB3', ['app.controllers']

module = angular.module 'app.controllers', ['app.services', 'ui.bootstrap']

module.controller 'startupsNetworkCtrl', ['$scope', 'dataAccess', ($scope, dataAccess)->
  dateFromHolder = $('#creation-date-from')
  dateToHolder = $('#creation-date-to')
  progressBar = $('#progress-bar')
  criteriaObject = {}
  $scope.searching = false
  interval = undefined
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
    startBar()
    criteriaObject = {}
    criteriaObject.location = if $scope.deepLocation then $scope.deepLocation else $scope.location
    criteriaObject.market = $scope.market
    dateFrom = dateFromHolder.val()
    dateTo = dateToHolder.val()
    criteriaObject.date = "(#{dateFrom},#{dateTo})" if dateFrom || dateTo
    criteriaObject.quality = "(#{$scope.qualityFrom},#{$scope.qualityTo})" if $scope.qualityFrom || $scope.qualityTo
    successHandler = (response) ->
      # check if the response has the actual data, if not the request must have been queued.
      containsData = response.startups and response.startups.length > 0 or response.rows and response.rows.length > 0
      if containsData
        $scope.startups = response.startups
        $scope.networkRows = response.rows
        $scope.exportURL = dataAccess.csv.url.startupsNetwork(criteriaObject)
        $scope.searching = false
        $scope.responseStatus = 'empty' unless response.startups.length > 0
      else
        $scope.responseStatus = 'queued'
      stopBar()
    errorHandler = -> stopBar(); $scope.responseStatus = 'error'
    dataAccess.startup.getNetwork criteriaObject, successHandler, errorHandler

  # Progress bar functions
  intervalFn = -> progressBar.css 'width', (index, value) ->
    value = parseInt(value.substring(0, value.length-1), 10)+1
    progressBar.css('width', "#{value}%") if value <= 100

  startBar = -> $scope.searching = true; interval = -> setInterval intervalFn, 2000

  stopBar = -> $scope.searching = false; clearInterval(interval); progressBar.css('width', '1%')
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