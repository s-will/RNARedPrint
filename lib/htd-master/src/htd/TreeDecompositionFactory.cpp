/*
 * File:   TreeDecompositionFactory.cpp
 *
 * Author: ABSEHER Michael (abseher@dbai.tuwien.ac.at)
 *
 * Copyright 2015-2017, Michael Abseher
 *    E-Mail: <abseher@dbai.tuwien.ac.at>
 *
 * This file is part of htd.
 *
 * htd is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * htd is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
 * License for more details.

 * You should have received a copy of the GNU General Public License
 * along with htd.  If not, see <http://www.gnu.org/licenses/>.
 */

#ifndef HTD_HTD_TREEDECOMPOSITIONFACTORY_CPP
#define HTD_HTD_TREEDECOMPOSITIONFACTORY_CPP

#include <htd/Globals.hpp>
#include <htd/TreeDecompositionFactory.hpp>
#include <htd/TreeDecomposition.hpp>

htd::TreeDecompositionFactory::TreeDecompositionFactory(const htd::LibraryInstance * const manager) : htd::GraphTypeFactory<htd::ITreeDecomposition, htd::IMutableTreeDecomposition>(new htd::TreeDecomposition(manager))
{

}

htd::TreeDecompositionFactory::~TreeDecompositionFactory()
{

}

htd::IMutableTreeDecomposition * htd::TreeDecompositionFactory::createInstance(void) const
{
#ifndef HTD_USE_VISUAL_STUDIO_COMPATIBILITY_MODE
    return constructionTemplate_->clone();
#else
    return constructionTemplate_->cloneMutableTreeDecomposition();
#endif
}

#endif /* HTD_HTD_TREEDECOMPOSITIONFACTORY_CPP */