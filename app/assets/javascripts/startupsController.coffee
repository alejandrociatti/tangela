angular.module 'AAC', ['app.controllers', 'ui.bootstrap']

module = angular.module 'app.controllers', ['app.services']

module.controller 'startupsCtrl', ['$scope', 'dataAccess', ($scope, dataAccess) ->
  dateFromHolder = $('#creation-date-from')
  dateToHolder = $('#creation-date-to')
  progressBar = $('#progress-bar')
  criteriaObject = {}
  $scope.searching = false
  interval = undefined
  $scope.qualityFrom = ''
  $scope.qualityTo = ''

  # Locations loader function
  $scope.getLocations = ->
    dataAccess.location.getChildren($scope.location).success((children) -> $scope.locations = children)

  # Form submit function
  $scope.submit = ->
    $scope.optionSelectMsg = 'Loading results...'
    $scope.searching = true
    interval = startBar()
    criteriaObject.location = if $scope.deepLocation then $scope.deepLocation else $scope.location
    criteriaObject.market = $scope.market
    dateFrom = dateFromHolder.val()
    dateTo = dateToHolder.val()
    criteriaObject.date = "(#{dateFrom},#{dateTo})" if dateFrom || dateTo
    criteriaObject.quality = "(#{$scope.qualityFrom},#{$scope.qualityTo})" if $scope.qualityFrom || $scope.qualityTo

    dataAccess.startup.getWithTags(criteriaObject).success((response) ->
      if(response.queued)
        $scope.responseStatus = 'queued'
      else
        $scope.startups = response.startups
        $scope.exportStartupsURL = dataAccess.csv.url.startups(criteriaObject)
        $scope.tags = response.tags
        $scope.exportStartupsTagsURL = dataAccess.csv.url.tags(criteriaObject)
        $scope.searching = false
        $scope.responseStatus = 'empty' unless response.startups.length > 0
        stopBar()
    ).error( ->
      stopBar()
      $scope.responseStatus = 'error'
    )

  # Progress bar functions
  intervalFn = -> progressBar.css 'width', (index, value) ->
    value = parseInt(value.substring(0, value.length-1), 10)+1
    progressBar.css('width', "#{value}%") if value <= 100

  startBar = -> setInterval intervalFn, 4000

  stopBar = -> clearInterval(interval); progressBar.css('width', '1%')
]

# Pagination controller
module.controller 'tableController', ['$scope', ($scope) ->
  $scope.itemsPerPage = 5
  $scope.currentPage = 1
  $scope.setPage = (pageNo) -> $scope.currentPage = pageNo
]

# Offset filter
module.filter 'offset', ->
  (input, start) -> (input || []).slice(parseInt(start, 10))