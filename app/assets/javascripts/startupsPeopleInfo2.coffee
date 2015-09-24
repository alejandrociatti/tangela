angular.module 'JB2', ['app.controllers']

module = angular.module 'app.controllers', ['app.services']

module.controller 'startupPeopleInfoCtrl', ['$scope', 'dataAccess', ($scope, dataAccess) ->
  dateFromHolder = $('#creation-date-from')
  dateToHolder = $('#creation-date-to')
  progressBar = $('#progress-bar')
  criteriaObject = {}
  $scope.searching = false
  interval = undefined
  $scope.people = []
  $scope.qualityFrom = ''
  $scope.qualityTo = ''

  # Locations loader function
  $scope.getLocations = ->
    dataAccess.location.getChildren($scope.location).success((children) -> $scope.locations = children)

  # Form submission handlers:
  successHandler = (response) ->
    if(response.queued)
      $scope.responseStatus = 'queued'
    else
      $scope.people = response.users
      $scope.exportURL = dataAccess.csv.url.users(criteriaObject)
      $scope.exportTagsURL = dataAccess.csv.url.usersTags(criteriaObject)
      $scope.searching = false
      $scope.responseStatus = 'empty' unless response.users.length > 0
    stopBar()

  errorHandler = ->
    stopBar()
    $scope.responseStatus = 'error'

  $scope.submit = ->
    startBar()
    dateFrom = dateFromHolder.val()
    dateTo = dateToHolder.val()
    criteriaObject =
      location: if $scope.deepLocation then $scope.deepLocation else $scope.location
      market: $scope.market
      date: "(#{dateFrom},#{dateTo})" if dateFrom || dateTo
      quality: "(#{$scope.qualityFrom},#{$scope.qualityTo})" if $scope.qualityFrom || $scope.qualityTo
    dataAccess.user.getByStartupCriteria(criteriaObject).success(successHandler).error(errorHandler)

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