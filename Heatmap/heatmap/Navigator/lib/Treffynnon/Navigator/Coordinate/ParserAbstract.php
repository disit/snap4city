<?php

/**
 * Navigator: a geographic calculation library for PHP
 * @link http://navigator.simonholywell.com
 * @license http://www.opensource.org/licenses/bsd-license.php BSD 2-Clause License 
 * @copyright 2012, Simon Holywell
 * @author Simon Holywell <treffynnon@php.net>
 */

namespace Treffynnon\Navigator\Coordinate;

use Treffynnon\Navigator as N;
use Treffynnon\Navigator\Exception as E;

/**
 * A base set of methods to set a direction (N,S,E,W) and validate
 * coordinates for classes that extend this abstract
 */
abstract class ParserAbstract implements ParserInterface {

    /**
     * The direction stored as either Treffynnon\Navigator::Long or Treffynnon\Navigator::Lat
     * @var string
     */
    protected $direction = null;

    /**
     * @inheritdoc
     */
    public function __construct($direction = null) {
        if (!is_null($direction)) {
            $this->setDirection($direction);
        }
    }

    /**
     * @inheritdoc
     */
    public function getDirection() {
        return $this->direction;
    }

    /**
     * @inheritdoc
     */
    public function setDirection($direction) {
        if ($direction === N::LAT
            or $direction === N::LONG) {
            $this->direction = $direction;
        } else {
            throw new E\InvalidDirectionException('You can only supply Treffynnon\Navigator::Long or Treffynnon\Navigator::Lat');
        }
    }

    /**
     * @inheritdoc
     */
    public function set($coord) {
        $radians = $this->parse($coord);
        if ($this->getDirection() == N::LAT
            and ($radians < -1.5707963267949 or $radians > 1.5707963267949)) {
            throw new E\InvalidCoordinateValueException('Latitude may not be greater than 90 or lower than -90');
        } elseif ($this->getDirection() == N::LONG
            and ($radians < -3.1415926535898 or $radians > 3.1415926535898)) {
            throw new E\InvalidCoordinateValueException('Longitude may not be greater than 180 or lower than -180');
        }
        return $radians;
    }

    abstract public function parse($coord);

    abstract public function get($coord);
}