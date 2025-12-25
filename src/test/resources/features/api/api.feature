Feature: API tests
  Scenario: GET a single post
    When I GET "/posts/1"
    Then status code should be 200
    And response JSON path "title" should not be empty

  Scenario: Create a post
    When I POST to "/posts" with body
      """
      {
        "title": "foo",
        "body": "bar",
        "userId": 1
      }
      """
    Then status code should be 201
    And response has integer id greater than 0

