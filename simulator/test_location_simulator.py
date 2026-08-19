import unittest

from location_simulator import distance_metres, interpolate_route


class RouteInterpolationTest(unittest.TestCase):
    def test_distance_is_reasonable_for_nairobi_coordinates(self) -> None:
        distance = distance_metres((36.8219, -1.2921), (36.8319, -1.2921))
        self.assertGreater(distance, 1_100)
        self.assertLess(distance, 1_120)

    def test_interpolation_keeps_route_endpoints(self) -> None:
        route = [[36.8219, -1.2921], [36.8319, -1.2921]]
        points = interpolate_route(route, 250)
        self.assertEqual(points[0], tuple(route[0]))
        self.assertEqual(points[-1], tuple(route[-1]))
        self.assertGreater(len(points), 4)

    def test_route_requires_two_points(self) -> None:
        with self.assertRaisesRegex(RuntimeError, "at least two"):
            interpolate_route([[36.8219, -1.2921]], 10)


if __name__ == "__main__":
    unittest.main()
